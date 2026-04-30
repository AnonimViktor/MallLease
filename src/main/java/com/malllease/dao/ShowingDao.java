package com.malllease.dao;

import com.malllease.model.Showing;
import com.malllease.model.ShowingRequestView;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShowingDao extends BaseDao<Showing> {

    @Override
    protected Showing mapRow(ResultSet rs) throws SQLException {
        Showing showing = new Showing();
        showing.setShowingId(rs.getInt("showing_id"));
        showing.setClientId(rs.getInt("client_id"));
        showing.setManagerUserId(rs.getInt("manager_user_id"));
        showing.setShownAt(rs.getTimestamp("shown_at").toLocalDateTime());
        showing.setResult(rs.getString("result"));
        showing.setComment(rs.getString("comment"));
        return showing;
    }

    public List<Showing> findAll() {
        return queryList("SELECT * FROM showing ORDER BY shown_at DESC");
    }

    public List<ShowingRequestView> findRequestViews() {
        String sql = """
            SELECT s.showing_id, s.client_id, c.company_name,
                   s.manager_user_id, u.full_name AS manager_name, s.shown_at,
                   COALESCE(s.result, 'requested') AS result, s.comment,
                   COALESCE(string_agg(tp.point_code, ', ' ORDER BY tp.point_code), '') AS point_codes,
                   COALESCE(string_agg(sc.name || ' / этаж ' || tp.floor, ', ' ORDER BY sc.name, tp.floor, tp.point_code), '') AS point_locations
            FROM showing s
            JOIN client c ON c.client_id = s.client_id
            JOIN users u ON u.user_id = s.manager_user_id
            LEFT JOIN showing_point sp ON sp.showing_id = s.showing_id
            LEFT JOIN trade_point tp ON tp.trade_point_id = sp.point_id
            LEFT JOIN shopping_center sc ON sc.shopping_center_id = tp.shopping_center_id
            GROUP BY s.showing_id, s.client_id, c.company_name,
                     s.manager_user_id, u.full_name, s.shown_at, s.result, s.comment
            ORDER BY s.shown_at ASC, s.showing_id ASC
            """;

        List<ShowingRequestView> rows = new ArrayList<>();
        try (var conn = borrowConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {
            while (rs.next()) {
                rows.add(mapRequestView(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Showing request query failed", e);
        }
        return rows;
    }

    private ShowingRequestView mapRequestView(ResultSet rs) throws SQLException {
        ShowingRequestView view = new ShowingRequestView();
        view.setShowingId(rs.getInt("showing_id"));
        view.setClientId(rs.getInt("client_id"));
        view.setCompanyName(rs.getString("company_name"));
        view.setManagerUserId(rs.getInt("manager_user_id"));
        view.setManagerName(rs.getString("manager_name"));
        view.setShownAt(rs.getTimestamp("shown_at").toLocalDateTime());
        view.setResult(rs.getString("result"));
        view.setComment(rs.getString("comment"));
        view.setPointCodes(rs.getString("point_codes"));
        view.setPointLocations(rs.getString("point_locations"));
        return view;
    }

    public List<Showing> findByClient(int clientId) {
        return queryList("SELECT * FROM showing WHERE client_id = ? ORDER BY shown_at DESC", clientId);
    }

    public Optional<Showing> findById(int showingId) {
        return querySingle("SELECT * FROM showing WHERE showing_id = ?", showingId);
    }

    public int create(Showing showing) {
        String sql = """
            INSERT INTO showing (client_id, manager_user_id, shown_at, result, comment)
            VALUES (?, ?, ?, ?, ?)
            """;
        return executeInsert(sql,
                showing.getClientId(),
                showing.getManagerUserId(),
                Timestamp.valueOf(showing.getShownAt()),
                showing.getResult(),
                showing.getComment());
    }

    public void addPoint(int showingId, int pointId) {
        executeUpdate(
                "INSERT INTO showing_point (showing_id, point_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                showingId,
                pointId);
    }

    public void updateResult(int showingId, String result, String comment) {
        executeUpdate(
                "UPDATE showing SET result = ?, comment = ? WHERE showing_id = ?",
                result,
                comment,
                showingId);
    }

    public void updateScheduleAndResult(int showingId, LocalDateTime shownAt, String result, String comment) {
        executeUpdate(
                "UPDATE showing SET shown_at = ?, result = ?, comment = ? WHERE showing_id = ?",
                Timestamp.valueOf(shownAt),
                result,
                comment,
                showingId);
    }

    public boolean isManagerSlotBusy(int managerUserId, LocalDateTime shownAt) {
        return isManagerSlotBusy(managerUserId, shownAt, -1);
    }

    public boolean isManagerSlotBusy(int managerUserId, LocalDateTime shownAt, int excludedShowingId) {
        String sql = """
            SELECT COUNT(*)
            FROM showing
            WHERE manager_user_id = ?
              AND shown_at = ?
              AND showing_id <> ?
              AND COALESCE(result, 'requested') <> 'refused'
            """;

        try (var conn = borrowConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, managerUserId);
            stmt.setTimestamp(2, Timestamp.valueOf(shownAt));
            stmt.setInt(3, excludedShowingId);
            try (var rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Slot check failed", e);
        }
    }

    public List<LocalTime> findBusySlotTimes(int managerUserId, LocalDate date, int excludedShowingId) {
        String sql = """
            SELECT shown_at
            FROM showing
            WHERE manager_user_id = ?
              AND shown_at >= ?
              AND shown_at < ?
              AND showing_id <> ?
              AND COALESCE(result, 'requested') <> 'refused'
            ORDER BY shown_at
            """;
        List<LocalTime> slots = new ArrayList<>();

        try (var conn = borrowConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, managerUserId);
            stmt.setTimestamp(2, Timestamp.valueOf(date.atStartOfDay()));
            stmt.setTimestamp(3, Timestamp.valueOf(date.plusDays(1).atStartOfDay()));
            stmt.setInt(4, excludedShowingId);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    slots.add(rs.getTimestamp("shown_at").toLocalDateTime().toLocalTime());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Busy slot query failed", e);
        }

        return slots;
    }
}
