package com.malllease.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class MonthlyCharge {
    private int chargeId;
    private int contractId;
    private int pointId;
    private LocalDate month;
    private BigDecimal amountDue;
    private String status;

    public MonthlyCharge() {}

    public int getChargeId() { return chargeId; }
    public void setChargeId(int chargeId) { this.chargeId = chargeId; }

    public int getContractId() { return contractId; }
    public void setContractId(int contractId) { this.contractId = contractId; }

    public int getPointId() { return pointId; }
    public void setPointId(int pointId) { this.pointId = pointId; }

    public LocalDate getMonth() { return month; }
    public void setMonth(LocalDate month) { this.month = month; }

    public BigDecimal getAmountDue() { return amountDue; }
    public void setAmountDue(BigDecimal amountDue) { this.amountDue = amountDue; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return chargeId == ((MonthlyCharge) o).chargeId;
    }

    @Override
    public int hashCode() { return Objects.hash(chargeId); }
}
