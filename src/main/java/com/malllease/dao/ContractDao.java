package com.malllease.dao;

import com.malllease.model.Contract;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

public class ContractDao extends BaseDao<Contract> {

    @Override
    protected Contract mapRow(ResultSet rs) throws SQLException {
        Contract contract = new Contract();
        contract.setContractId(rs.getInt("contract_id"));
        contract.setClientId(rs.getInt("client_id"));
        contract.setContractNo(rs.getString("contract_no"));
        contract.setSignedAt(rs.getDate("signed_at").toLocalDate());
        contract.setStatus(rs.getString("status"));
        contract.setComment(rs.getString("comment"));
        return contract;
    }

    public List<Contract> findAll() {
        return queryList("SELECT * FROM contract ORDER BY signed_at DESC");
    }

    public List<Contract> findByClient(int clientId) {
        return queryList(
                "SELECT * FROM contract WHERE client_id = ? ORDER BY signed_at DESC",
                clientId);
    }

    public Optional<Contract> findById(int contractId) {
        return querySingle("SELECT * FROM contract WHERE contract_id = ?", contractId);
    }

    public int create(Contract contract) {
        return executeInsert(
                """
                INSERT INTO contract (client_id, contract_no, signed_at, status, comment)
                VALUES (?, ?, ?, ?, ?)
                """,
                contract.getClientId(),
                contract.getContractNo(),
                Date.valueOf(contract.getSignedAt()),
                contract.getStatus(),
                contract.getComment());
    }

    public void updateStatus(int contractId, String status) {
        executeUpdate(
                "UPDATE contract SET status = ? WHERE contract_id = ?",
                status, contractId);
    }

    public Optional<Contract> findByShowing(int showingId) {
        String suffix = "-S" + String.format("%04d", showingId);
        return querySingle(
                """
                SELECT * FROM contract
                WHERE contract_no LIKE '%' || ?
                ORDER BY (status = 'active') DESC, contract_id DESC
                LIMIT 1
                """,
                suffix);
    }

    public void terminateContract(int contractId) {
        runInTransaction(connection -> {
            try {
                try (PreparedStatement rentals = connection.prepareStatement(
                        "UPDATE contract_rental SET status = 'terminated' WHERE contract_id = ? AND status = 'active'")) {
                    rentals.setInt(1, contractId);
                    rentals.executeUpdate();
                }
                try (PreparedStatement contract = connection.prepareStatement(
                        "UPDATE contract SET status = 'terminated' WHERE contract_id = ?")) {
                    contract.setInt(1, contractId);
                    contract.executeUpdate();
                }
                return null;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to terminate contract", e);
            }
        });
    }

    public int createFromShowing(int showingId, DateRange rentalPeriod, String managerComment) {
        if (rentalPeriod == null || rentalPeriod.dateFrom().isAfter(rentalPeriod.dateTo())) {
            throw new IllegalArgumentException("Проверьте период аренды");
        }
        return runInTransaction(connection -> {
            try {
                ShowingContractSource source = loadShowingSource(connection, showingId);
                if ("contract_signed".equals(source.result())) {
                    throw new IllegalArgumentException("По этому показу договор уже заключен");
                }

                List<PointLeaseSource> points = loadShowingPoints(connection, showingId);
                if (points.isEmpty()) {
                    throw new IllegalArgumentException("У показа нет выбранных торговых точек");
                }
                for (PointLeaseSource point : points) {
                    if (!point.active()) {
                        throw new IllegalArgumentException("Точка " + point.pointCode() + " закрыта и недоступна для аренды");
                    }
                    if (hasOverlappingActiveRental(connection, point.pointId(), rentalPeriod)) {
                        throw new IllegalArgumentException("Точка " + point.pointCode() + " уже занята в выбранный период");
                    }
                }

                int contractId = insertContractFromShowing(connection, showingId, source, rentalPeriod, managerComment);
                insertContractRentals(connection, contractId, points, rentalPeriod);
                generateMonthlyCharges(connection, contractId);
                markShowingSigned(connection, showingId, managerComment);
                return contractId;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to create contract from showing", e);
            }
        });
    }

    private ShowingContractSource loadShowingSource(Connection connection, int showingId) throws SQLException {
        String sql = """
                SELECT client_id, COALESCE(result, 'requested') AS result
                FROM showing
                WHERE showing_id = ?
                """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, showingId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new ShowingContractSource(
                            rs.getInt("client_id"),
                            rs.getString("result"));
                }
            }
        }
        throw new IllegalArgumentException("Показ не найден");
    }

    private List<PointLeaseSource> loadShowingPoints(Connection connection, int showingId) throws SQLException {
        String sql = """
                SELECT tp.trade_point_id, tp.point_code, tp.current_daily_rate, tp.is_active
                FROM showing_point sp
                JOIN trade_point tp ON tp.trade_point_id = sp.point_id
                WHERE sp.showing_id = ?
                ORDER BY tp.point_code
                """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, showingId);
            try (ResultSet rs = stmt.executeQuery()) {
                java.util.ArrayList<PointLeaseSource> points = new java.util.ArrayList<>();
                while (rs.next()) {
                    points.add(new PointLeaseSource(
                            rs.getInt("trade_point_id"),
                            rs.getString("point_code"),
                            rs.getBigDecimal("current_daily_rate"),
                            rs.getBoolean("is_active")));
                }
                return points;
            }
        }
    }

    private boolean hasOverlappingActiveRental(Connection connection, int pointId, DateRange rentalPeriod)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM contract_rental cr
                JOIN contract ct ON ct.contract_id = cr.contract_id
                WHERE cr.point_id = ?
                  AND ct.status = 'active'
                  AND cr.status = 'active'
                  AND NOT (? > cr.date_to OR ? < cr.date_from)
                """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, pointId);
            stmt.setDate(2, Date.valueOf(rentalPeriod.dateFrom()));
            stmt.setDate(3, Date.valueOf(rentalPeriod.dateTo()));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private int insertContractFromShowing(
            Connection connection,
            int showingId,
            ShowingContractSource source,
            DateRange rentalPeriod,
            String managerComment) throws SQLException {
        String sql = """
                INSERT INTO contract (client_id, contract_no, signed_at, status, comment)
                VALUES (?, ?, CURRENT_DATE, 'active', ?)
                """;
        String contractNo = "ML-" + rentalPeriod.dateFrom().getYear() + "-S" + String.format("%04d", showingId);
        String comment = commentOrDefault(managerComment, "Создано по показу #" + showingId);

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, source.clientId());
            stmt.setString(2, contractNo);
            stmt.setString(3, comment);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Contract id was not generated");
    }

    private void insertContractRentals(
            Connection connection,
            int contractId,
            List<PointLeaseSource> points,
            DateRange rentalPeriod) throws SQLException {
        String sql = """
                INSERT INTO contract_rental (contract_id, point_id, date_from, date_to, daily_rate_fixed, status)
                VALUES (?, ?, ?, ?, ?, 'active')
                """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (PointLeaseSource point : points) {
                stmt.setInt(1, contractId);
                stmt.setInt(2, point.pointId());
                stmt.setDate(3, Date.valueOf(rentalPeriod.dateFrom()));
                stmt.setDate(4, Date.valueOf(rentalPeriod.dateTo()));
                stmt.setBigDecimal(5, point.dailyRate());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void generateMonthlyCharges(Connection connection, int contractId) throws SQLException {
        String sql = """
                INSERT INTO monthly_charges (contract_id, point_id, month, amount_due, status)
                SELECT cr.contract_id, cr.point_id, gs::date,
                       cr.daily_rate_fixed
                         * ((LEAST((gs + INTERVAL '1 month - 1 day')::date, cr.date_to)) - gs::date + 1),
                       'unpaid'
                FROM contract_rental cr
                CROSS JOIN LATERAL generate_series(cr.date_from::timestamp, cr.date_to::timestamp,
                                                   INTERVAL '1 month') AS gs
                WHERE cr.contract_id = ?
                """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, contractId);
            stmt.executeUpdate();
        }
    }

    private void markShowingSigned(Connection connection, int showingId, String managerComment) throws SQLException {
        String sql = "UPDATE showing SET result = 'contract_signed', comment = ? WHERE showing_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, commentOrDefault(managerComment, "Договор заключен"));
            stmt.setInt(2, showingId);
            stmt.executeUpdate();
        }
    }

    private String commentOrDefault(String comment, String defaultComment) {
        return comment == null || comment.isBlank() ? defaultComment : comment.trim();
    }

    public record DateRange(java.time.LocalDate dateFrom, java.time.LocalDate dateTo) {}

    public java.util.List<com.malllease.model.ClientContractCard> findCardsByClientUser(int clientUserId) {
        String sql = """
                SELECT ct.contract_id, ct.contract_no, ct.signed_at, ct.status, ct.comment,
                       tp.trade_point_id, tp.point_code, sc.name AS center_name, tp.floor,
                       cr.date_from, cr.date_to, cr.daily_rate_fixed,
                       COALESCE((SELECT SUM(p.amount)
                                 FROM monthly_charges mc
                                 JOIN payment p ON p.charge_id = mc.charge_id
                                 WHERE mc.contract_id = cr.contract_id
                                   AND mc.point_id = cr.point_id), 0) AS paid_total
                FROM contract ct
                JOIN client c           ON c.client_id      = ct.client_id
                JOIN contract_rental cr ON cr.contract_id   = ct.contract_id
                JOIN trade_point tp     ON tp.trade_point_id = cr.point_id
                JOIN shopping_center sc ON sc.shopping_center_id = tp.shopping_center_id
                WHERE c.user_id = ?
                ORDER BY ct.signed_at DESC, ct.contract_id DESC, tp.point_code
                """;

        java.util.LinkedHashMap<Integer, com.malllease.model.ClientContractCard> byId = new java.util.LinkedHashMap<>();
        java.util.Map<String, com.malllease.model.ClientContractCard.Rental> rentalLookup = new java.util.HashMap<>();
        try (java.sql.Connection conn = borrowConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clientUserId);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int cid = rs.getInt("contract_id");
                    com.malllease.model.ClientContractCard card = byId.get(cid);
                    if (card == null) {
                        card = new com.malllease.model.ClientContractCard();
                        card.setContractId(cid);
                        card.setContractNo(rs.getString("contract_no"));
                        java.sql.Date signed = rs.getDate("signed_at");
                        card.setSignedAt(signed == null ? null : signed.toLocalDate());
                        card.setStatus(rs.getString("status"));
                        card.setComment(rs.getString("comment"));
                        byId.put(cid, card);
                    }
                    int pointId = rs.getInt("trade_point_id");
                    java.time.LocalDate from = rs.getDate("date_from").toLocalDate();
                    java.time.LocalDate to   = rs.getDate("date_to").toLocalDate();
                    java.math.BigDecimal rate = rs.getBigDecimal("daily_rate_fixed");
                    java.math.BigDecimal paid = rs.getBigDecimal("paid_total");
                    long days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
                    java.math.BigDecimal expected = rate.multiply(java.math.BigDecimal.valueOf(days));
                    com.malllease.model.ClientContractCard.Rental rental =
                            new com.malllease.model.ClientContractCard.Rental(
                                    pointId,
                                    rs.getString("point_code"),
                                    rs.getString("center_name"),
                                    rs.getInt("floor"),
                                    from, to, rate, paid, expected);
                    card.getRentals().add(rental);
                    rentalLookup.put(cid + ":" + pointId, rental);
                }
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Client contract cards query failed", e);
        }

        attachPaymentHistory(clientUserId, rentalLookup);
        return new java.util.ArrayList<>(byId.values());
    }

    private void attachPaymentHistory(int clientUserId,
                                      java.util.Map<String, com.malllease.model.ClientContractCard.Rental> rentalLookup) {
        if (rentalLookup.isEmpty()) {
            return;
        }
        String sql = """
                SELECT mc.contract_id, mc.point_id, mc.month, p.paid_at, p.amount
                FROM monthly_charges mc
                JOIN payment p ON p.charge_id = mc.charge_id
                JOIN contract ct ON ct.contract_id = mc.contract_id
                JOIN client c ON c.client_id = ct.client_id
                WHERE c.user_id = ?
                ORDER BY p.paid_at DESC, p.payment_id DESC
                """;
        try (java.sql.Connection conn = borrowConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clientUserId);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getInt("contract_id") + ":" + rs.getInt("point_id");
                    com.malllease.model.ClientContractCard.Rental rental = rentalLookup.get(key);
                    if (rental == null) {
                        continue;
                    }
                    java.sql.Date month = rs.getDate("month");
                    java.sql.Date paidAt = rs.getDate("paid_at");
                    rental.payments.add(new com.malllease.model.ClientContractCard.PaymentEntry(
                            paidAt == null ? null : paidAt.toLocalDate(),
                            rs.getBigDecimal("amount"),
                            month == null ? null : month.toLocalDate()));
                }
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Client payment history query failed", e);
        }
    }

    private record ShowingContractSource(int clientId, String result) {}

    private record PointLeaseSource(int pointId, String pointCode, BigDecimal dailyRate, boolean active) {}
}
