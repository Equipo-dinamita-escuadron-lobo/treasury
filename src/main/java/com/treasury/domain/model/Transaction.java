package com.treasury.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Transaction {
    private Long id;
    private LocalDateTime date;
    private String description;
    private String documentType;
    private Long documentId;
    private String userId;
    private LocalDateTime createdAt;
    private List<Split> splits;

    public Transaction(LocalDateTime date, String description, String documentType,
                      Long documentId, String userId) {
        this.date = date;
        this.description = description;
        this.documentType = documentType;
        this.documentId = documentId;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
    }

    public void validateBalance() {
        if (splits == null || splits.isEmpty()) {
            throw new IllegalStateException("Transaction must have at least one split");
        }

        BigDecimal total = splits.stream()
                .map(Split::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Transaction splits must balance to zero");
        }
    }

    public BigDecimal getTotalDebits() {
        return splits.stream()
                .map(Split::getAmount)
                .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalCredits() {
        return splits.stream()
                .map(Split::getAmount)
                .filter(amount -> amount.compareTo(BigDecimal.ZERO) < 0)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
