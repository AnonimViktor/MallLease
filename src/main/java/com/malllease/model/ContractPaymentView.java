package com.malllease.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ContractPaymentView {
    private int contractId;
    private String contractNo;
    private String contractStatus;
    private String companyName;
    private int clientUserId;
    private int pointId;
    private String pointCode;
    private LocalDate rentalFrom;
    private LocalDate rentalTo;
    private BigDecimal dailyRate;
    private BigDecimal paidTotal;
    private LocalDate lastPaidTo;
    private LocalDate nextPeriodFrom;
    private LocalDate nextPeriodTo;
    private BigDecimal amountDue;
    private int chargeId;
    private boolean hasCharges;

    public int getContractId() { return contractId; }
    public void setContractId(int contractId) { this.contractId = contractId; }

    public String getContractNo() { return contractNo; }
    public void setContractNo(String contractNo) { this.contractNo = contractNo; }

    public String getContractStatus() { return contractStatus; }
    public void setContractStatus(String contractStatus) { this.contractStatus = contractStatus; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public int getClientUserId() { return clientUserId; }
    public void setClientUserId(int clientUserId) { this.clientUserId = clientUserId; }

    public int getPointId() { return pointId; }
    public void setPointId(int pointId) { this.pointId = pointId; }

    public String getPointCode() { return pointCode; }
    public void setPointCode(String pointCode) { this.pointCode = pointCode; }

    public LocalDate getRentalFrom() { return rentalFrom; }
    public void setRentalFrom(LocalDate rentalFrom) { this.rentalFrom = rentalFrom; }

    public LocalDate getRentalTo() { return rentalTo; }
    public void setRentalTo(LocalDate rentalTo) { this.rentalTo = rentalTo; }

    public BigDecimal getDailyRate() { return dailyRate; }
    public void setDailyRate(BigDecimal dailyRate) { this.dailyRate = dailyRate; }

    public BigDecimal getPaidTotal() { return paidTotal; }
    public void setPaidTotal(BigDecimal paidTotal) { this.paidTotal = paidTotal; }

    public LocalDate getLastPaidTo() { return lastPaidTo; }
    public void setLastPaidTo(LocalDate lastPaidTo) { this.lastPaidTo = lastPaidTo; }

    public LocalDate getNextPeriodFrom() { return nextPeriodFrom; }
    public void setNextPeriodFrom(LocalDate nextPeriodFrom) { this.nextPeriodFrom = nextPeriodFrom; }

    public LocalDate getNextPeriodTo() { return nextPeriodTo; }
    public void setNextPeriodTo(LocalDate nextPeriodTo) { this.nextPeriodTo = nextPeriodTo; }

    public BigDecimal getAmountDue() { return amountDue; }
    public void setAmountDue(BigDecimal amountDue) { this.amountDue = amountDue; }

    public int getChargeId() { return chargeId; }
    public void setChargeId(int chargeId) { this.chargeId = chargeId; }

    public boolean hasCharges() { return hasCharges; }
    public void setHasCharges(boolean hasCharges) { this.hasCharges = hasCharges; }

    public boolean isPayable() {
        return "active".equals(contractStatus)
                && nextPeriodFrom != null
                && nextPeriodTo != null
                && amountDue != null
                && amountDue.compareTo(BigDecimal.ZERO) > 0;
    }
}
