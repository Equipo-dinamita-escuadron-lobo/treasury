package com.treasury.infrastructure.adapters.output.jpa.repository;

import com.treasury.infrastructure.adapters.output.jpa.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    List<TransactionEntity> findByUserId(String userId);

    List<TransactionEntity> findByDocumentTypeAndDocumentId(String documentType, Long documentId);

    @Query("SELECT t FROM TransactionEntity t WHERE t.date BETWEEN :startDate AND :endDate ORDER BY t.date DESC")
    List<TransactionEntity> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM TransactionEntity t WHERE t.userId = :userId AND t.date BETWEEN :startDate AND :endDate ORDER BY t.date DESC")
    List<TransactionEntity> findByUserIdAndDateRange(@Param("userId") String userId,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);
}
