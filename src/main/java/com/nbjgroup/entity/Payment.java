package com.nbjgroup.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Payment entity representing rent payments and other financial transactions.
 * Tracks payment history, status, and provides comprehensive payment management.
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_tenant", columnList = "tenant_id"),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_date", columnList = "payment_date"),
    @Index(name = "idx_payment_due_date", columnList = "due_date"),
    @Index(name = "idx_payment_type", columnList = "payment_type")
})
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many-to-One relationship with Tenant
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonBackReference("tenant-payment")
    private Tenant tenant;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Due date is required")
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private PaymentType paymentType = PaymentType.RENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "reference_number")
    private String referenceNumber;

    @Column(name = "late_fee", precision = 10, scale = 2)
    private BigDecimal lateFee = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_period_start")
    private LocalDate paymentPeriodStart;

    @Column(name = "payment_period_end")
    private LocalDate paymentPeriodEnd;

    @Column(name = "receipt_path")
    private String receiptPath;

    @Column(name = "receipt_name")
    private String receiptName;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "processed_by")
    private String processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public Payment() {}

    public Payment(Tenant tenant, BigDecimal amount, LocalDate dueDate, PaymentType paymentType) {
        this.tenant = tenant;
        this.amount = amount;
        this.dueDate = dueDate;
        this.paymentType = paymentType;
        this.status = PaymentStatus.PENDING;
        this.totalAmount = amount;
    }

    // Business methods
    public boolean isOverdue() {
        return status == PaymentStatus.PENDING && LocalDate.now().isAfter(dueDate);
    }

    public long getDaysOverdue() {
        if (!isOverdue()) return 0;
        return dueDate.until(LocalDate.now()).getDays();
    }

    public BigDecimal calculateLateFee(BigDecimal lateFeeRate, int gracePeriodDays) {
        if (!isOverdue()) return BigDecimal.ZERO;
        
        long daysOverdue = getDaysOverdue();
        if (daysOverdue <= gracePeriodDays) return BigDecimal.ZERO;
        
        return amount.multiply(lateFeeRate).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public void applyLateFee(BigDecimal lateFeeAmount) {
        this.lateFee = lateFeeAmount;
        this.totalAmount = amount.add(lateFee).subtract(discountAmount);
    }

    public void applyDiscount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
        this.totalAmount = amount.add(lateFee).subtract(discountAmount);
    }

    public void markAsPaid(PaymentMethod method, String transactionId, String processedBy) {
        this.status = PaymentStatus.COMPLETED;
        this.paymentDate = LocalDate.now();
        this.paymentMethod = method;
        this.transactionId = transactionId;
        this.processedBy = processedBy;
        this.processedAt = LocalDateTime.now();
    }

    public void markAsFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.notes = (notes != null ? notes + "\n" : "") + "Payment failed: " + reason;
    }

    public void cancel(String reason) {
        this.status = PaymentStatus.CANCELLED;
        this.notes = (notes != null ? notes + "\n" : "") + "Payment cancelled: " + reason;
    }

    public boolean canBeModified() {
        return status == PaymentStatus.PENDING || status == PaymentStatus.PARTIAL;
    }

    public String getPaymentPeriodDescription() {
        if (paymentPeriodStart != null && paymentPeriodEnd != null) {
            return paymentPeriodStart + " to " + paymentPeriodEnd;
        }
        return "N/A";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
        // Recalculate total amount when base amount changes
        if (this.totalAmount == null) {
            this.totalAmount = amount;
        }
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public BigDecimal getLateFee() {
        return lateFee;
    }

    public void setLateFee(BigDecimal lateFee) {
        this.lateFee = lateFee;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDate getPaymentPeriodStart() {
        return paymentPeriodStart;
    }

    public void setPaymentPeriodStart(LocalDate paymentPeriodStart) {
        this.paymentPeriodStart = paymentPeriodStart;
    }

    public LocalDate getPaymentPeriodEnd() {
        return paymentPeriodEnd;
    }

    public void setPaymentPeriodEnd(LocalDate paymentPeriodEnd) {
        this.paymentPeriodEnd = paymentPeriodEnd;
    }

    public String getReceiptPath() {
        return receiptPath;
    }

    public void setReceiptPath(String receiptPath) {
        this.receiptPath = receiptPath;
    }

    public String getReceiptName() {
        return receiptName;
    }

    public void setReceiptName(String receiptName) {
        this.receiptName = receiptName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return Objects.equals(id, payment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", amount=" + amount +
                ", dueDate=" + dueDate +
                ", status=" + status +
                ", paymentType=" + paymentType +
                '}';
    }

    /**
     * Enum representing payment status
     */
    public enum PaymentStatus {
        PENDING("Pending"),
        COMPLETED("Completed"),
        PARTIAL("Partial"),
        FAILED("Failed"),
        CANCELLED("Cancelled"),
        REFUNDED("Refunded");

        private final String displayName;

        PaymentStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Enum representing payment types
     */
    public enum PaymentType {
        RENT("Monthly Rent"),
        SECURITY_DEPOSIT("Security Deposit"),
        LATE_FEE("Late Fee"),
        UTILITY("Utility Payment"),
        MAINTENANCE("Maintenance Fee"),
        OTHER("Other");

        private final String displayName;

        PaymentType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Enum representing payment methods
     */
    public enum PaymentMethod {
        CASH("Cash"),
        CHECK("Check"),
        BANK_TRANSFER("Bank Transfer"),
        CREDIT_CARD("Credit Card"),
        DEBIT_CARD("Debit Card"),
        ONLINE_PAYMENT("Online Payment"),
        MOBILE_PAYMENT("Mobile Payment");

        private final String displayName;

        PaymentMethod(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
