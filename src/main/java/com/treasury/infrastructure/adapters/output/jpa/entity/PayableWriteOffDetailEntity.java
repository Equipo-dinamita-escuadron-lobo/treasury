package com.treasury.infrastructure.adapters.output.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;

@Entity @Table(name="payable_write_off_details")
@Getter @Setter @NoArgsConstructor
public class PayableWriteOffDetailEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @JsonIgnore @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="write_off_id",nullable=false) private PayableWriteOffEntity writeOff;
    @Column(name="supplier_id",nullable=false) private Long supplierId;
    @Column(name="invoice_id",nullable=false) private Long invoiceId;
    @Column(name="payable_account_id",nullable=false) private Long payableAccountId;
    @Column(name="payable_account_code",nullable=false,length=30) private String payableAccountCode;
    @Column(nullable=false,precision=19,scale=2) private BigDecimal amount;
    @TenantId @Column(name="tenant_id",nullable=false,length=80) private String tenantId;
}
