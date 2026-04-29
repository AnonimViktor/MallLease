package com.malllease.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Showing {
    private int showingId;
    private int clientId;
    private int managerUserId;
    private LocalDateTime shownAt;
    private String result;
    private String comment;

    public Showing() {}

    public int getShowingId() { return showingId; }
    public void setShowingId(int showingId) { this.showingId = showingId; }

    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    public int getManagerUserId() { return managerUserId; }
    public void setManagerUserId(int managerUserId) { this.managerUserId = managerUserId; }

    public LocalDateTime getShownAt() { return shownAt; }
    public void setShownAt(LocalDateTime shownAt) { this.shownAt = shownAt; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Showing showing = (Showing) o;
        return showingId == showing.showingId;
    }

    @Override
    public int hashCode() { return Objects.hash(showingId); }

    @Override
    public String toString() { return "Showing #" + showingId; }
}
