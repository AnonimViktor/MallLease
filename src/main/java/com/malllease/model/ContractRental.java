package com.malllease.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class ContractRental {
    private int contractId;
    private int pointId;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private BigDecimal dailyRateFixed;
    private String status;

    public ContractRental() {}

    public int getContractId() { return contractId; }
    public void setContractId(int contractId) { this.contractId = contractId; }

    public int getPointId() { return pointId; }
    public void setPointId(int pointId) { this.pointId = pointId; }

    public LocalDate getDateFrom() { return dateFrom; }
    public void setDateFrom(LocalDate dateFrom) { this.dateFrom = dateFrom; }

    public LocalDate getDateTo() { return dateTo; }
    public void setDateTo(LocalDate dateTo) { this.dateTo = dateTo; }

    public BigDecimal getDailyRateFixed() { return dailyRateFixed; }
    public void setDailyRateFixed(BigDecimal dailyRateFixed) { this.dailyRateFixed = dailyRateFixed; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContractRental that = (ContractRental) o;
        return contractId == that.contractId && pointId == that.pointId;
    }

    @Override
    public int hashCode() { return Objects.hash(contractId, pointId); }
}
