package com.malllease.model;

import java.util.Objects;

public class Role {
    private int roleId;
    private String code;
    private String name;

    public Role() {}

    public Role(int roleId, String code, String name) {
        this.roleId = roleId;
        this.code = code;
        this.name = name;
    }

    public int getRoleId() { return roleId; }
    public void setRoleId(int roleId) { this.roleId = roleId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return roleId == role.roleId;
    }

    @Override
    public int hashCode() { return Objects.hash(roleId); }

    @Override
    public String toString() { return name + " (" + code + ")"; }
}
