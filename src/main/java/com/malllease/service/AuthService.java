package com.malllease.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.malllease.config.Database;
import com.malllease.model.Role;
import com.malllease.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService {

    public record RegistrationRequest(
            String login,
            String email,
            String phone,
            String fullName,
            String password,
            String companyName,
            String legalAddress,
            String bankDetails
    ) {}

    public User registerClient(RegistrationRequest request) {
        String passwordHash = BCrypt.withDefaults()
                .hashToString(10, request.password().toCharArray());

        try (Connection connection = Database.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                int roleId = findRoleId(connection, "client");
                int userId = insertUser(connection, request, roleId, passwordHash);
                insertClient(connection, request, userId);
                connection.commit();

                Role role = new Role(roleId, "client", "Клиент");
                User user = new User();
                user.setUserId(userId);
                user.setRoleId(roleId);
                user.setRole(role);
                user.setLogin(request.login());
                user.setEmail(request.email());
                user.setPhone(request.phone());
                user.setFullName(request.fullName());
                user.setPasswordHash(passwordHash);
                user.setActive(true);
                return user;
            } catch (SQLException | RuntimeException e) {
                try { connection.rollback(); } catch (SQLException ignored) {}
                throw e;
            } finally {
                try { connection.setAutoCommit(previousAutoCommit); } catch (SQLException ignored) {}
            }
        } catch (SQLException e) {
            throw new RuntimeException("Client registration failed", e);
        }
    }

    private int findRoleId(Connection connection, String code) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT role_id FROM role WHERE code = ?")) {
            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("role_id");
                }
            }
        }
        throw new SQLException("Role not found: " + code);
    }

    private int insertUser(Connection connection, RegistrationRequest request, int roleId, String passwordHash)
            throws SQLException {
        String sql = """
            INSERT INTO users (role_id, login, email, phone, full_name, password_hash, is_active)
            VALUES (?, ?, ?, ?, ?, ?, TRUE)
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, roleId);
            stmt.setString(2, request.login());
            stmt.setString(3, request.email());
            stmt.setString(4, blankToNull(request.phone()));
            stmt.setString(5, request.fullName());
            stmt.setString(6, passwordHash);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Generated user id was not returned");
    }

    private void insertClient(Connection connection, RegistrationRequest request, int userId) throws SQLException {
        String sql = """
            INSERT INTO client (user_id, company_name, legal_address, bank_details, status)
            VALUES (?, ?, ?, ?, 'active')
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, request.companyName());
            stmt.setString(3, blankToNull(request.legalAddress()));
            stmt.setString(4, blankToNull(request.bankDetails()));
            stmt.executeUpdate();
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
