package com.lodge.model;

import com.lodge.util.Money;

import java.time.LocalDateTime;

/** A single payment received against a bill. */
public class Payment {

    private String paymentId;
    private String billId;
    private double amount;
    private PaymentMethod method;
    private String reference;
    private String receivedBy;
    private LocalDateTime timestamp;

    public Payment() {
        this.timestamp = LocalDateTime.now();
        this.method = PaymentMethod.CASH;
    }

    public Payment(String paymentId, String billId, double amount, PaymentMethod method, String reference, String receivedBy) {
        this();
        this.paymentId = paymentId;
        this.billId = billId;
        this.amount = Money.round(amount);
        this.method = method;
        this.reference = reference;
        this.receivedBy = receivedBy;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getBillId() {
        return billId;
    }

    public void setBillId(String billId) {
        this.billId = billId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getFormattedAmount() {
        return Money.format(amount);
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public String getMethodLabel() {
        return method == null ? "" : method.getLabel();
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(String receivedBy) {
        this.receivedBy = receivedBy;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedDate() {
        return timestamp == null ? "" : timestamp.format(com.lodge.util.JsonAdapters.DATE_TIME);
    }
}
