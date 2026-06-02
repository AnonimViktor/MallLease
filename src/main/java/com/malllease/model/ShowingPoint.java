package com.malllease.model;

import java.util.Objects;

/**
 * Composite key entity linking a Showing to the TradePoints demonstrated.
 */
public class ShowingPoint {
    private int showingId;
    private int pointId;

    public ShowingPoint() {}

    public ShowingPoint(int showingId, int pointId) {
        this.showingId = showingId;
        this.pointId = pointId;
    }

    public int getShowingId() { return showingId; }
    public void setShowingId(int showingId) { this.showingId = showingId; }

    public int getPointId() { return pointId; }
    public void setPointId(int pointId) { this.pointId = pointId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShowingPoint that = (ShowingPoint) o;
        return showingId == that.showingId && pointId == that.pointId;
    }

    @Override
    public int hashCode() { return Objects.hash(showingId, pointId); }
}
