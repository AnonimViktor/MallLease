package com.malllease.model;

public class TradePointAvailability {

    public enum Status { FREE, OCCUPIED_BY_SELF, OCCUPIED_BY_OTHER, UNAVAILABLE }

    private final int tradePointId;
    private final Status status;
    private final String occupiedByCompany;
    private final java.time.LocalDate occupiedFrom;
    private final java.time.LocalDate occupiedTo;

    public TradePointAvailability(int tradePointId, Status status,
                                  String occupiedByCompany,
                                  java.time.LocalDate from,
                                  java.time.LocalDate to) {
        this.tradePointId = tradePointId;
        this.status = status;
        this.occupiedByCompany = occupiedByCompany;
        this.occupiedFrom = from;
        this.occupiedTo = to;
    }

    public int getTradePointId() { return tradePointId; }
    public Status getStatus() { return status; }
    public String getOccupiedByCompany() { return occupiedByCompany; }
    public java.time.LocalDate getOccupiedFrom() { return occupiedFrom; }
    public java.time.LocalDate getOccupiedTo() { return occupiedTo; }
}
