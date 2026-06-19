package com.malllease.model;

import java.math.BigDecimal;
import java.util.Objects;

public class TradePoint {
    private int tradePointId;
    private int shoppingCenterId;
    private String pointCode;
    private int floor;
    private BigDecimal areaM2;
    private boolean hasAirConditioner;
    private BigDecimal currentDailyRate;
    private boolean active = true;
    private String imageUrl;

    private String status;

    public TradePoint() {}

    public int getTradePointId() { return tradePointId; }
    public void setTradePointId(int tradePointId) { this.tradePointId = tradePointId; }

    public int getShoppingCenterId() { return shoppingCenterId; }
    public void setShoppingCenterId(int shoppingCenterId) { this.shoppingCenterId = shoppingCenterId; }

    public String getPointCode() { return pointCode; }
    public void setPointCode(String pointCode) { this.pointCode = pointCode; }

    public int getFloor() { return floor; }
    public void setFloor(int floor) { this.floor = floor; }

    public BigDecimal getAreaM2() { return areaM2; }
    public void setAreaM2(BigDecimal areaM2) { this.areaM2 = areaM2; }

    public boolean isHasAirConditioner() { return hasAirConditioner; }
    public void setHasAirConditioner(boolean hasAirConditioner) { this.hasAirConditioner = hasAirConditioner; }

    public BigDecimal getCurrentDailyRate() { return currentDailyRate; }
    public void setCurrentDailyRate(BigDecimal currentDailyRate) { this.currentDailyRate = currentDailyRate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TradePoint that = (TradePoint) o;
        return tradePointId == that.tradePointId;
    }

    @Override
    public int hashCode() { return Objects.hash(tradePointId); }

    @Override
    public String toString() { return pointCode + " (этаж " + floor + ")"; }
}
