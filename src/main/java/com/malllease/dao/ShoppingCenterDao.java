package com.malllease.dao;

import com.malllease.model.ShoppingCenter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ShoppingCenterDao extends BaseDao<ShoppingCenter> {

    @Override
    protected ShoppingCenter mapRow(ResultSet rs) throws SQLException {
        ShoppingCenter center = new ShoppingCenter();
        center.setShoppingCenterId(rs.getInt("shopping_center_id"));
        center.setName(rs.getString("name"));
        center.setAddress(rs.getString("address"));
        center.setImageUrl(rs.getString("image_url"));
        center.setMapPath(rs.getString("map_path"));
        return center;
    }

    public List<ShoppingCenter> findAll() {
        return queryList("SELECT * FROM shopping_center ORDER BY name");
    }

    public Optional<ShoppingCenter> findById(int id) {
        return querySingle("SELECT * FROM shopping_center WHERE shopping_center_id = ?", id);
    }

    public int create(ShoppingCenter center) {
        return executeInsert(
                "INSERT INTO shopping_center (name, address, image_url, map_path) VALUES (?, ?, ?, ?)",
                center.getName(),
                center.getAddress(),
                center.getImageUrl(),
                center.getMapPath());
    }

    public void update(ShoppingCenter center) {
        executeUpdate(
                "UPDATE shopping_center SET name = ?, address = ?, image_url = ?, map_path = ? WHERE shopping_center_id = ?",
                center.getName(),
                center.getAddress(),
                center.getImageUrl(),
                center.getMapPath(),
                center.getShoppingCenterId());
    }

    public void delete(int id) {
        executeUpdate("DELETE FROM shopping_center WHERE shopping_center_id = ?", id);
    }
}
