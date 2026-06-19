package com.malllease.dao;

import com.malllease.model.ContractPaymentView;
import com.malllease.model.Payment;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PaymentDao extends BaseDao<Payment> {

    private static final DateTimeFormatter DOCUMENT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    protected Payment mapRow(ResultSet rs) throws SQLException {
        Payment payment = new Payment();
        payment.setPaymentId(rs.getInt("payment_id"));
        payment.setChargeId(rs.getInt("charge_id"));
        payment.setPaidAt(rs.getDate("paid_at").toLocalDate());
        payment.setAmount(rs.getBigDecimal("amount"));
        payment.setDocumentNo(rs.getString("document_no"));
        payment.setComment(rs.getString("comment"));
        try {
            Date month = rs.getDate("charge_month");
            if (month != null) {
                payment.setChargeMonth(month.toLocalDate());
            }
        } catch (SQLException ignored) {
        }
        return payment;
    }

    public List<Payment> findByRental(int contractId, int pointId) {
        return queryList(
                """
                SELECT p.*, mc.month AS charge_month
                FROM payment p
                JOIN monthly_charges mc ON mc.charge_id = p.charge_id
                WHERE mc.contract_id = ? AND mc.point_id = ?
                ORDER BY p.paid_at DESC, p.payment_id DESC
                """,
                contractId, pointId);
    }

    public int create(Payment payment) {
        return executeInsert(
                """
                INSERT INTO payment (charge_id, paid_at, amount, document_no, comment)
                VALUES (?, ?, ?, ?, ?)
                """,
                payment.getChargeId(),
                Date.valueOf(payment.getPaidAt()),
                payment.getAmount(),
                payment.getDocumentNo(),
                payment.getComment());
    }

    public List<ContractPaymentView> findContractPaymentViewsForClientUser(int userId) {
        return findContractPaymentViews("AND c.user_id = ?", userId);
    }

    public List<ContractPaymentView> findContractPaymentViewsForWorkplace() {
        return findContractPaymentViews("", new Object[0]);
    }

    public int payCurrentPeriod(ContractPaymentView view, String comment, BigDecimal amount) {
        if (view == null || !view.isPayable()) {
            throw new IllegalArgumentException("По выбранной строке нет периода к оплате");
        }
        BigDecimal finalAmount = amount == null ? view.getAmountDue() : amount;
        if (finalAmount.signum() <= 0) {
            throw new IllegalArgumentException("Сумма платежа должна быть больше нуля");
        }
        if (finalAmount.compareTo(view.getAmountDue()) > 0) {
            throw new IllegalArgumentException("Сумма платежа не может превышать остаток по периоду");
        }

        return runInTransaction(connection -> {
            try {
                Payment payment = new Payment();
                payment.setChargeId(view.getChargeId());
                payment.setPaidAt(LocalDate.now());
                payment.setAmount(finalAmount);
                payment.setDocumentNo(documentNo(view));
                payment.setComment(comment);

                int paymentId;
                try (PreparedStatement stmt = connection.prepareStatement(
                        "INSERT INTO payment (charge_id, paid_at, amount, document_no, comment) VALUES (?, ?, ?, ?, ?)",
                        java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setInt(1, payment.getChargeId());
                    stmt.setDate(2, Date.valueOf(payment.getPaidAt()));
                    stmt.setBigDecimal(3, payment.getAmount());
                    stmt.setString(4, payment.getDocumentNo());
                    stmt.setString(5, payment.getComment());
                    stmt.executeUpdate();
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        paymentId = keys.next() ? keys.getInt(1) : 0;
                    }
                }
                closeChargeIfCovered(connection, view.getChargeId());
                return paymentId;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to register payment", e);
            }
        });
    }

    private void closeChargeIfCovered(Connection connection, int chargeId) throws SQLException {
        String sql = """
                UPDATE monthly_charges mc
                SET status = 'paid'
                WHERE mc.charge_id = ?
                  AND mc.amount_due <= (SELECT COALESCE(SUM(p.amount), 0)
                                        FROM payment p WHERE p.charge_id = mc.charge_id)
                """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, chargeId);
            stmt.executeUpdate();
        }
    }

    private List<ContractPaymentView> findContractPaymentViews(String extraWhere, Object... params) {
        String sql = """
                SELECT ct.contract_id, ct.contract_no, ct.status AS contract_status,
                       c.company_name, c.user_id AS client_user_id,
                       cr.point_id, tp.point_code, cr.date_from AS rental_from, cr.date_to AS rental_to,
                       cr.daily_rate_fixed,
                       mc.charge_id, mc.month AS charge_month, mc.amount_due,
                       COALESCE(SUM(p.amount), 0) AS charge_paid
                FROM contract_rental cr
                JOIN contract ct ON ct.contract_id = cr.contract_id
                JOIN client c ON c.client_id = ct.client_id
                JOIN trade_point tp ON tp.trade_point_id = cr.point_id
                LEFT JOIN monthly_charges mc ON mc.contract_id = cr.contract_id AND mc.point_id = cr.point_id
                LEFT JOIN payment p ON p.charge_id = mc.charge_id
                WHERE ct.status = 'active'
                  AND cr.status = 'active'
                """ + "\n" + extraWhere + "\n" + """
                GROUP BY ct.contract_id, ct.contract_no, ct.status, c.company_name, c.user_id,
                         cr.point_id, tp.point_code, cr.date_from, cr.date_to, cr.daily_rate_fixed,
                         mc.charge_id, mc.month, mc.amount_due
                ORDER BY c.company_name, ct.contract_no, tp.point_code, mc.month
                """;

        Map<String, ContractPaymentView> byRental = new LinkedHashMap<>();
        try (Connection conn = borrowConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getInt("contract_id") + ":" + rs.getInt("point_id");
                    ContractPaymentView view = byRental.computeIfAbsent(key, k -> newView(rs));
                    accumulateCharge(view, rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Contract payment view query failed", e);
        }
        return new ArrayList<>(byRental.values());
    }

    private ContractPaymentView newView(ResultSet rs) {
        try {
            ContractPaymentView view = new ContractPaymentView();
            view.setContractId(rs.getInt("contract_id"));
            view.setContractNo(rs.getString("contract_no"));
            view.setContractStatus(rs.getString("contract_status"));
            view.setCompanyName(rs.getString("company_name"));
            view.setClientUserId(rs.getInt("client_user_id"));
            view.setPointId(rs.getInt("point_id"));
            view.setPointCode(rs.getString("point_code"));
            view.setRentalFrom(rs.getDate("rental_from").toLocalDate());
            view.setRentalTo(rs.getDate("rental_to").toLocalDate());
            view.setDailyRate(rs.getBigDecimal("daily_rate_fixed"));
            view.setPaidTotal(BigDecimal.ZERO);
            view.setAmountDue(BigDecimal.ZERO);
            view.setChargeId(0);
            return view;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void accumulateCharge(ContractPaymentView view, ResultSet rs) throws SQLException {
        int chargeId = rs.getInt("charge_id");
        if (rs.wasNull() || chargeId == 0) {
            return;
        }
        view.setHasCharges(true);
        BigDecimal amountDue = rs.getBigDecimal("amount_due");
        BigDecimal chargePaid = rs.getBigDecimal("charge_paid");
        view.setPaidTotal(view.getPaidTotal().add(chargePaid));

        BigDecimal remaining = amountDue.subtract(chargePaid);
        if (view.getChargeId() == 0 && remaining.signum() > 0) {
            LocalDate month = rs.getDate("charge_month").toLocalDate();
            LocalDate periodTo = month.plusMonths(1).minusDays(1);
            if (periodTo.isAfter(view.getRentalTo())) {
                periodTo = view.getRentalTo();
            }
            view.setChargeId(chargeId);
            view.setNextPeriodFrom(month);
            view.setNextPeriodTo(periodTo);
            view.setAmountDue(remaining);
        }
    }

    private String documentNo(ContractPaymentView view) {
        return "PAY-" + LocalDateTime.now().format(DOCUMENT_FORMATTER)
                + "-" + view.getContractId()
                + "-" + view.getPointId();
    }
}
