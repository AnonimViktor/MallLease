package com.malllease.model;

import java.time.LocalDate;
import java.util.Objects;

public class Contract {
    private int contractId;
    private int clientId;
    private String contractNo;
    private LocalDate signedAt;
    private String status;
    private String comment;

    public Contract() {}

    public int getContractId() { return contractId; }
    public void setContractId(int contractId) { this.contractId = contractId; }

    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    public String getContractNo() { return contractNo; }
    public void setContractNo(String contractNo) { this.contractNo = contractNo; }

    public LocalDate getSignedAt() { return signedAt; }
    public void setSignedAt(LocalDate signedAt) { this.signedAt = signedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Contract contract = (Contract) o;
        return contractId == contract.contractId;
    }

    @Override
    public int hashCode() { return Objects.hash(contractId); }

    @Override
    public String toString() { return "Договор " + contractNo; }
}
