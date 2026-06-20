package com.treasury.infrastructure.adapters.output.jpa.entity;

import org.hibernate.annotations.TenantId;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "splits")
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class SplitEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", insertable = false, updatable = false)
    private Long transactionId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount; // Positivo = débito, negativo = crédito

    @Column(name = "memo")
    private String memo;

    @TenantId
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", referencedColumnName = "id")
    private TransactionEntity transaction;
}
