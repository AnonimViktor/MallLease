package com.malllease.dao;

import com.malllease.model.Client;
import com.malllease.model.Role;
import com.malllease.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ClientDao extends BaseDao<Client> {

    @Override
    protected Client mapRow(ResultSet rs) throws SQLException {
        Client client = new Client();
        client.setClientId(rs.getInt("client_id"));
        client.setUserId(rs.getInt("user_id"));
        client.setCompanyName(rs.getString("company_name"));
        client.setLegalAddress(rs.getString("legal_address"));
        client.setBankDetails(rs.getString("bank_details"));
        client.setStatus(rs.getString("status"));

        try {
            User user = new User();
            user.setUserId(rs.getInt("user_id"));
            user.setRoleId(rs.getInt("role_id"));
            user.setLogin(rs.getString("login"));
            user.setEmail(rs.getString("email"));
            user.setPhone(rs.getString("phone"));
            user.setFullName(rs.getString("full_name"));
            user.setActive(rs.getBoolean("is_active"));

            Role role = new Role();
            role.setRoleId(rs.getInt("role_id"));
            role.setCode(rs.getString("role_code"));
            role.setName(rs.getString("role_name"));
            user.setRole(role);
            client.setUser(user);
        } catch (SQLException ignored) {
        }

        return client;
    }

    public List<Client> findAll() {
        String sql = """
            SELECT c.*, u.login, u.email, u.phone, u.full_name, u.role_id, u.is_active,
                   r.code AS role_code, r.name AS role_name
            FROM client c
            JOIN users u ON u.user_id = c.user_id
            JOIN role r ON r.role_id = u.role_id
            ORDER BY c.company_name
            """;
        return queryList(sql);
    }

    public Optional<Client> findById(int clientId) {
        return querySingle("SELECT * FROM client WHERE client_id = ?", clientId);
    }

    public Optional<Client> findByUserId(int userId) {
        return querySingle("SELECT * FROM client WHERE user_id = ?", userId);
    }

    public int create(Client client) {
        String sql = """
            INSERT INTO client (user_id, company_name, legal_address, bank_details, status)
            VALUES (?, ?, ?, ?, ?)
            """;
        return executeInsert(sql,
                client.getUserId(),
                client.getCompanyName(),
                client.getLegalAddress(),
                client.getBankDetails(),
                client.getStatus());
    }

    public void update(Client client) {
        String sql = """
            UPDATE client
            SET company_name = ?, legal_address = ?, bank_details = ?, status = ?
            WHERE client_id = ?
            """;
        executeUpdate(sql,
                client.getCompanyName(),
                client.getLegalAddress(),
                client.getBankDetails(),
                client.getStatus(),
                client.getClientId());
    }

    public void delete(int clientId) {
        executeUpdate("DELETE FROM client WHERE client_id = ?", clientId);
    }
}
