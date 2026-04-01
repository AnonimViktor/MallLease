package com.malllease.dao;

import com.malllease.model.TradePoint;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class TradePointDao extends BaseDao<TradePoint> {

    private static final String SELECT_BASE = """
            SELECT tp.trade_point_id, tp.shopping_center_id, tp.point_code, tp.floor,
                   tp.area_m2, tp.has_air_conditioner, tp.current_daily_rate, tp.is_active, tp.image_url,
                   CASE
                       WHEN NOT tp.is_active THEN 'unavailable'
                       WHEN EXISTS (
                           SELECT 1 FROM contract_rental cr
                           WHERE cr.point_id = tp.trade_point_id
                             AND cr.status = 'active'
                             AND CURRENT_DATE BETWEEN cr.date_from AND cr.date_to
                       ) THEN 'occupied'
                       ELSE 'free'
                   END AS status
            FROM trade_point tp
            """;

    @Override
    protected TradePoint mapRow(ResultSet rs) throws SQLException {
        TradePoint point = new TradePoint();
        point.setTradePointId(rs.getInt("trade_point_id"));
        point.setShoppingCenterId(rs.getInt("shopping_center_id"));
        point.setPointCode(rs.getString("point_code"));
        point.setFloor(rs.getInt("floor"));
        point.setAreaM2(rs.getBigDecimal("area_m2"));
        point.setHasAirConditioner(rs.getBoolean("has_air_conditioner"));
        point.setCurrentDailyRate(rs.getBigDecimal("current_daily_rate"));
        point.setActive(rs.getBoolean("is_active"));
        point.setImageUrl(rs.getString("image_url"));
        point.setStatus(rs.getString("status"));
        return point;
    }

    public List<TradePoint> findByShoppingCenter(int shoppingCenterId) {
        return queryList(
                SELECT_BASE + " WHERE tp.shopping_center_id = ? ORDER BY tp.floor, tp.point_code",
                shoppingCenterId);
    }

    public List<TradePoint> findByShoppingCenterAndFloor(int shoppingCenterId, int floor) {
        return queryList(
                SELECT_BASE + " WHERE tp.shopping_center_id = ? AND tp.floor = ? ORDER BY tp.point_code",
                shoppingCenterId, floor);
    }

    public List<Integer> findDistinctFloors(int shoppingCenterId) {
        List<Integer> floors = new java.util.ArrayList<>();
        String sql = "SELECT DISTINCT floor FROM trade_point WHERE shopping_center_id = ? ORDER BY floor";
        try (var conn = borrowConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, shoppingCenterId);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    floors.add(rs.getInt("floor"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Query failed", e);
        }
        return floors;
    }

    public Optional<TradePoint> findById(int id) {
        return querySingle(SELECT_BASE + " WHERE tp.trade_point_id = ?", id);
    }

    public int create(TradePoint point) {
        return executeInsert(
                """
                INSERT INTO trade_point (shopping_center_id, point_code, floor, area_m2,
                    has_air_conditioner, current_daily_rate, is_active, image_url)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                point.getShoppingCenterId(),
                point.getPointCode(),
                point.getFloor(),
                point.getAreaM2(),
                point.isHasAirConditioner(),
                point.getCurrentDailyRate(),
                point.isActive(),
                point.getImageUrl());
    }

    public void setActive(int tradePointId, boolean active) {
        executeUpdate(
                "UPDATE trade_point SET is_active = ? WHERE trade_point_id = ?",
                active, tradePointId);
    }

    public List<com.malllease.model.TradePointAvailability> findAvailability(
            int shoppingCenterId, int floor,
            java.time.LocalDate from, java.time.LocalDate to,
            int currentClientId) {

        String sql = """
                SELECT tp.trade_point_id,
                       tp.is_active             AS tp_active,
                       MAX(cr.contract_id)      AS contract_id,
                       BOOL_OR(ct.client_id = ?) AS owned_by_self,
                       MIN(c.company_name)      AS occupier_name,
                       MIN(cr.date_from)        AS occ_from,
                       MAX(cr.date_to)          AS occ_to,
                       COUNT(cr.point_id)       AS overlap_count
                FROM trade_point tp
                LEFT JOIN contract_rental cr
                       ON cr.point_id = tp.trade_point_id
                      AND cr.status = 'active'
                      AND NOT (cr.date_from > ? OR cr.date_to < ?)
                LEFT JOIN contract ct ON ct.contract_id = cr.contract_id AND ct.status = 'active'
                LEFT JOIN client   c  ON c.client_id   = ct.client_id
                WHERE tp.shopping_center_id = ?
                  AND tp.floor = ?
                GROUP BY tp.trade_point_id, tp.is_active
                """;

        java.util.List<com.malllease.model.TradePointAvailability> out = new java.util.ArrayList<>();
        try (java.sql.Connection conn = borrowConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentClientId);
            stmt.setObject(2, to);
            stmt.setObject(3, from);
            stmt.setInt(4, shoppingCenterId);
            stmt.setInt(5, floor);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("trade_point_id");
                    boolean tpActive = rs.getBoolean("tp_active");
                    int overlap = rs.getInt("overlap_count");
                    com.malllease.model.TradePointAvailability.Status status;
                    String occupier = null;
                    java.time.LocalDate occFrom = null, occTo = null;
                    if (!tpActive) {
                        status = com.malllease.model.TradePointAvailability.Status.UNAVAILABLE;
                    } else if (overlap == 0) {
                        status = com.malllease.model.TradePointAvailability.Status.FREE;
                    } else if (rs.getBoolean("owned_by_self")) {
                        status = com.malllease.model.TradePointAvailability.Status.OCCUPIED_BY_SELF;
                        occupier = rs.getString("occupier_name");
                        java.sql.Date d1 = rs.getDate("occ_from");
                        java.sql.Date d2 = rs.getDate("occ_to");
                        if (d1 != null) occFrom = d1.toLocalDate();
                        if (d2 != null) occTo = d2.toLocalDate();
                    } else {
                        status = com.malllease.model.TradePointAvailability.Status.OCCUPIED_BY_OTHER;
                        occupier = rs.getString("occupier_name");
                    }
                    out.add(new com.malllease.model.TradePointAvailability(
                            id, status, occupier, occFrom, occTo));
                }
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Availability query failed", e);
        }
        return out;
    }
}
