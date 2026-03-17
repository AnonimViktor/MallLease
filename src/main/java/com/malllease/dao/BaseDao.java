package com.malllease.dao;

import com.malllease.config.Database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class BaseDao<T> {

    private static final Logger log = LoggerFactory.getLogger(BaseDao.class);

    protected abstract T mapRow(ResultSet rs) throws SQLException;

    protected Connection borrowConnection() throws SQLException {
        return Database.getConnection();
    }

    protected List<T> queryList(String sql, Object... params) {
        try (Connection conn = borrowConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameters(stmt, params);
            List<T> results = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
            return results;
        } catch (SQLException e) {
            throw fail("Query failed", sql, e);
        }
    }

    protected Optional<T> querySingle(String sql, Object... params) {
        try (Connection conn = borrowConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw fail("Query failed", sql, e);
        }
    }

    protected int executeUpdate(String sql, Object... params) {
        try (Connection conn = borrowConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameters(stmt, params);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw fail("Update failed", sql, e);
        }
    }

    protected int executeInsert(String sql, Object... params) {
        try (Connection conn = borrowConnection();
             PreparedStatement stmt = conn.prepareStatement(sql,
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
            setParameters(stmt, params);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw fail("Insert failed", sql, e);
        }
        return -1;
    }

    protected <R> R runInTransaction(Function<Connection, R> work) {
        try (Connection conn = borrowConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                R result = work.apply(conn);
                conn.commit();
                return result;
            } catch (RuntimeException | Error e) {
                safeRollback(conn);
                throw e;
            } finally {
                try { conn.setAutoCommit(previousAutoCommit); } catch (SQLException ignored) {}
            }
        } catch (SQLException e) {
            throw new RuntimeException("Transaction failed: " + e.getMessage(), e);
        }
    }

    private void safeRollback(Connection conn) {
        try { conn.rollback(); }
        catch (SQLException e) { log.warn("Rollback failed", e); }
    }

    private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }

    private RuntimeException fail(String stage, String sql, SQLException e) {
        log.error("{} — {}: {}", stage, e.getMessage(), oneLine(sql));
        return new RuntimeException(stage + ": " + sql, e);
    }

    private String oneLine(String sql) {
        return sql == null ? "" : sql.replaceAll("\\s+", " ").trim();
    }
}
