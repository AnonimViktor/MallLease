package com.malllease.dao;

import com.malllease.model.Role;
import com.malllease.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class UserDao extends BaseDao<User> {

    @Override
    protected User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setRoleId(rs.getInt("role_id"));
        user.setLogin(rs.getString("login"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setFullName(rs.getString("full_name"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setActive(rs.getBoolean("is_active"));

        Role role = new Role();
        role.setRoleId(rs.getInt("role_id"));
        try {
            role.setCode(rs.getString("role_code"));
            role.setName(rs.getString("role_name"));
        } catch (SQLException ignored) {
        }
        user.setRole(role);

        return user;
    }

    public Optional<User> findByLogin(String login) {
        String sql = """
            SELECT u.*, r.code AS role_code, r.name AS role_name
            FROM users u
            JOIN role r ON r.role_id = u.role_id
            WHERE u.login = ? AND u.is_active = TRUE
            """;
        return querySingle(sql, login);
    }

    public Optional<User> findById(int userId) {
        String sql = """
            SELECT u.*, r.code AS role_code, r.name AS role_name
            FROM users u
            JOIN role r ON r.role_id = u.role_id
            WHERE u.user_id = ?
            """;
        return querySingle(sql, userId);
    }

    public List<User> findAll() {
        String sql = """
            SELECT u.*, r.code AS role_code, r.name AS role_name
            FROM users u
            JOIN role r ON r.role_id = u.role_id
            ORDER BY u.full_name
        """;
        return queryList(sql);
    }

    public List<User> findByRoleCode(String roleCode) {
        String sql = """
            SELECT u.*, r.code AS role_code, r.name AS role_name
            FROM users u
            JOIN role r ON r.role_id = u.role_id
            WHERE r.code = ? AND u.is_active = TRUE
            ORDER BY u.full_name
            """;
        return queryList(sql, roleCode);
    }

    public Optional<User> findFirstByRoleCode(String roleCode) {
        String sql = """
            SELECT u.*, r.code AS role_code, r.name AS role_name
            FROM users u
            JOIN role r ON r.role_id = u.role_id
            WHERE r.code = ? AND u.is_active = TRUE
            ORDER BY u.user_id
            LIMIT 1
            """;
        return querySingle(sql, roleCode);
    }

    public int create(User user) {
        String sql = """
            INSERT INTO users (role_id, login, email, phone, full_name, password_hash, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        return executeInsert(sql,
                user.getRoleId(),
                user.getLogin(),
                user.getEmail(),
                user.getPhone(),
                user.getFullName(),
                user.getPasswordHash(),
                user.isActive());
    }

    public void update(User user) {
        String sql = """
            UPDATE users
            SET role_id = ?, login = ?, email = ?, phone = ?, full_name = ?, is_active = ?
            WHERE user_id = ?
            """;
        executeUpdate(sql,
                user.getRoleId(),
                user.getLogin(),
                user.getEmail(),
                user.getPhone(),
                user.getFullName(),
                user.isActive(),
                user.getUserId());
    }

    public void updatePasswordHash(int userId, String passwordHash) {
        executeUpdate("UPDATE users SET password_hash = ? WHERE user_id = ?", passwordHash, userId);
    }

    public void deactivate(int userId) {
        executeUpdate("UPDATE users SET is_active = FALSE WHERE user_id = ?", userId);
    }

    public boolean existsByLogin(String login) {
        return countOf("SELECT COUNT(*) FROM users WHERE login = ?", login) > 0;
    }

    public boolean existsByLoginExceptUser(String login, int userId) {
        return countOf("SELECT COUNT(*) FROM users WHERE login = ? AND user_id <> ?",
                login, userId) > 0;
    }

    public boolean existsByEmail(String email) {
        return countOf("SELECT COUNT(*) FROM users WHERE email = ?", email) > 0;
    }

    public boolean existsByEmailExceptUser(String email, int userId) {
        return countOf("SELECT COUNT(*) FROM users WHERE email = ? AND user_id <> ?",
                email, userId) > 0;
    }

    private int countOf(String sql, Object... params) {
        try (java.sql.Connection conn = borrowConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Count query failed: " + sql, e);
        }
    }
}
