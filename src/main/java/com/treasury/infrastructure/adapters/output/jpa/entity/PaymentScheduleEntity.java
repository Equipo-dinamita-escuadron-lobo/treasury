package com.treasury.infrastructure.adapters.output.jpa.entity;

import com.treasury.domain.model.PaymentScheduleStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name="payment_schedules")
@Getter @Setter @NoArgsConstructor
public class PaymentScheduleEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="enterprise_id",nullable=false,length=80) private String enterpriseId;
    @Column(name="execution_date",nullable=false) private LocalDate executionDate;
    @Column(name="payment_method_id",nullable=false) private Long paymentMethodId;
    @Column(name="bank_account_id") private Long bankAccountId;
    @Column(length=500) private String observations;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private PaymentScheduleStatus status=PaymentScheduleStatus.SCHEDULED;
    @Column(name="voucher_id") private Long voucherId;
    @Column(name="retry_count",nullable=false) private int retryCount;
    @Column(name="failure_reason",length=500) private String failureReason;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Version private long version;
    @TenantId @Column(name="tenant_id",nullable=false,length=80) private String tenantId;
    @OneToMany(mappedBy="schedule",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.EAGER)
    private List<PaymentScheduleDetailEntity> details=new ArrayList<>();
    @Transient public BigDecimal getTotal(){return details.stream().map(PaymentScheduleDetailEntity::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);}
    @PrePersist void created(){createdAt=updatedAt=Instant.now();}
    @PreUpdate void updated(){updatedAt=Instant.now();}
}
