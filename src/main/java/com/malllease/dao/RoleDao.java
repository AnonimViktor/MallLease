package com.malllease.dao;

import com.malllease.model.Role;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class RoleDao extends BaseDao<Role> {

    @Override
    protected Role mapRow(ResultSet rs) throws SQLException {
        Role role = new Role();
        role.setRoleId(rs.getInt("role_id"));
        role.setCode(rs.getString("code"));
        role.setName(rs.getString("name"));
        return role;
    }

    public List<Role> findAll() {
        return queryList("SELECT * FROM role ORDER BY role_id");
    }

    public Optional<Role> findByCode(String code) {
        return querySingle("SELECT * FROM role WHERE code = ?", code);
    }
}
