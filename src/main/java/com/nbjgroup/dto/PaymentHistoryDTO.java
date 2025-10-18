package com.nbjgroup.dto;

import com.nbjgroup.entity.Payment;
import com.nbjgroup.entity.Payment.PaymentMethod;
import com.nbjgroup.entity.Payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PaymentHistoryDTO {
    private Long id;
    private String tenantName;
    private BigDecimal amount;
    private LocalDate dueDate;
    private LocalDate paymentDate;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private String propertyAddress;
    private String unitNumber;

    public PaymentHistoryDTO() {}

    public PaymentHistoryDTO(Payment payment) {
        this.id = payment.getId();
        this.tenantName = payment.getTenant().getUser().getFirstName() + " " + payment.getTenant().getUser().getLastName();
        this.amount = payment.getAmount();
        this.dueDate = payment.getDueDate();
        this.paymentDate = payment.getPaymentDate();
        this.status = payment.getStatus();
        this.paymentMethod = payment.getPaymentMethod();
        this.propertyAddress = payment.getTenant().getPropertyAddress();
        this.unitNumber = payment.getTenant().getUnitNumber();
    }

    // Getters and Setters (omitted for brevity)
}