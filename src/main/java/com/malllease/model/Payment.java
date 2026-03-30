package com.malllease.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class Payment {
    private int paymentId;
    private int chargeId;
    private LocalDate paidAt;
    private BigDecimal amount;
    private String documentNo;
    private String comment;
    private LocalDate chargeMonth;

    public Payment() {}

    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }

    public int getChargeId() { return chargeId; }
    public void setChargeId(int chargeId) { this.chargeId = chargeId; }

    public LocalDate getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDate paidAt) { this.paidAt = paidAt; }

    public LocalDate getChargeMonth() { return chargeMonth; }
    public void setChargeMonth(LocalDate chargeMonth) { this.chargeMonth = chargeMonth; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDocumentNo() { return documentNo; }
    public void setDocumentNo(String documentNo) { this.documentNo = documentNo; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return paymentId == payment.paymentId;
    }

    @Override
    public int hashCode() { return Objects.hash(paymentId); }

    @Override
    public String toString() { return "Платёж #" + paymentId + " (" + amount + " руб.)"; }
}
