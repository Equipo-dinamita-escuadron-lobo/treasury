package com.treasury.infrastructure.adapters.output.jpa.repository;

import com.treasury.infrastructure.adapters.output.jpa.entity.SplitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SplitRepository extends JpaRepository<SplitEntity, Long> {

    List<SplitEntity> findByTransactionId(Long transactionId);

    List<SplitEntity> findByAccountId(Long accountId);

    @Query("SELECT s FROM SplitEntity s WHERE s.accountId = :accountId AND s.transaction.date BETWEEN :startDate AND :endDate")
    List<SplitEntity> findByAccountIdAndDateRange(@Param("accountId") Long accountId,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(s.amount) FROM SplitEntity s WHERE s.accountId = :accountId")
    BigDecimal getAccountBalance(@Param("accountId") Long accountId);

    @Query("SELECT SUM(s.amount) FROM SplitEntity s WHERE s.accountId = :accountId AND s.transaction.date <= :asOfDate")
    BigDecimal getAccountBalanceAsOf(@Param("accountId") Long accountId, @Param("asOfDate") LocalDateTime asOfDate);
}
