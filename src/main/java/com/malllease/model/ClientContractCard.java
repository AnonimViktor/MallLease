package com.malllease.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ClientContractCard {

    public record PaymentEntry(LocalDate paidAt, BigDecimal amount, LocalDate month) {}

    public static class Rental {
        public final int pointId;
        public final String pointCode;
        public final String shoppingCenterName;
        public final int floor;
        public final LocalDate dateFrom;
        public final LocalDate dateTo;
        public final BigDecimal dailyRate;
        public final BigDecimal paidTotal;
        public final BigDecimal expectedTotal;
        public final List<PaymentEntry> payments = new ArrayList<>();

        public Rental(int pointId, String pointCode, String shoppingCenterName, int floor,
                      LocalDate dateFrom, LocalDate dateTo,
                      BigDecimal dailyRate, BigDecimal paidTotal, BigDecimal expectedTotal) {
            this.pointId = pointId;
            this.pointCode = pointCode;
            this.shoppingCenterName = shoppingCenterName;
            this.floor = floor;
            this.dateFrom = dateFrom;
            this.dateTo = dateTo;
            this.dailyRate = dailyRate;
            this.paidTotal = paidTotal;
            this.expectedTotal = expectedTotal;
        }
    }

    private int contractId;
    private String contractNo;
    private LocalDate signedAt;
    private String status;
    private String comment;
    private List<Rental> rentals = new ArrayList<>();

    public int getContractId() { return contractId; }
    public void setContractId(int v) { contractId = v; }
    public String getContractNo() { return contractNo; }
    public void setContractNo(String v) { contractNo = v; }
    public LocalDate getSignedAt() { return signedAt; }
    public void setSignedAt(LocalDate v) { signedAt = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public String getComment() { return comment; }
    public void setComment(String v) { comment = v; }
    public List<Rental> getRentals() { return rentals; }

    public BigDecimal totalPaid() {
        return rentals.stream().map(r -> r.paidTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalExpected() {
        return rentals.stream().map(r -> r.expectedTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
