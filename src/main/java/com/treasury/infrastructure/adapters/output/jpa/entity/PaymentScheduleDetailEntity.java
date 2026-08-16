package com.treasury.infrastructure.adapters.output.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;

@Entity @Table(name="payment_schedule_details",uniqueConstraints=@UniqueConstraint(name="uk_schedule_invoice",columnNames={"schedule_id","invoice_id"}))
@Getter @Setter @NoArgsConstructor
public class PaymentScheduleDetailEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @JsonIgnore @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="schedule_id",nullable=false) private PaymentScheduleEntity schedule;
    @Column(name="supplier_id",nullable=false) private Long supplierId;
    @Column(name="invoice_id",nullable=false) private Long invoiceId;
    @Column(nullable=false,precision=19,scale=2) private BigDecimal amount;
    @Column(nullable=false) private boolean canceled = false;
    @Column(name="cancellation_reason",length=500) private String cancellationReason;
    @TenantId @Column(name="tenant_id",nullable=false,length=80) private String tenantId;
}
