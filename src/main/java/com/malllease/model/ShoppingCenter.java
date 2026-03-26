package com.malllease.model;

import java.util.Objects;

public class ShoppingCenter {
    private int shoppingCenterId;
    private String name;
    private String address;
    private String imageUrl;
    private String mapPath;

    public ShoppingCenter() {}

    public ShoppingCenter(int shoppingCenterId, String name, String address) {
        this.shoppingCenterId = shoppingCenterId;
        this.name = name;
        this.address = address;
    }

    public int getShoppingCenterId() { return shoppingCenterId; }
    public void setShoppingCenterId(int shoppingCenterId) { this.shoppingCenterId = shoppingCenterId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getMapPath() { return mapPath; }
    public void setMapPath(String mapPath) { this.mapPath = mapPath; }

    public boolean hasMap() { return mapPath != null && !mapPath.isBlank(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShoppingCenter that = (ShoppingCenter) o;
        return shoppingCenterId == that.shoppingCenterId;
    }

    @Override
    public int hashCode() { return Objects.hash(shoppingCenterId); }

    @Override
    public String toString() { return name; }
}
