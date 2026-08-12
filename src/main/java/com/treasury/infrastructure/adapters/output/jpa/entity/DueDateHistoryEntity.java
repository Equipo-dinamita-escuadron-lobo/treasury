package com.treasury.infrastructure.adapters.output.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.time.LocalDate;

@Entity @Table(name="due_date_history")
@Getter @Setter @NoArgsConstructor
public class DueDateHistoryEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="invoice_id",nullable=false) private Long invoiceId;
    @Column(name="previous_date",nullable=false) private LocalDate previousDate;
    @Column(name="new_date",nullable=false) private LocalDate newDate;
    @Column(nullable=false,length=500) private String reason;
    @Column(name="changed_at",nullable=false) private Instant changedAt=Instant.now();
    @Column(name="changed_by",nullable=false,length=100) private String changedBy;
    @TenantId @Column(name="tenant_id",nullable=false,length=80) private String tenantId;
}
