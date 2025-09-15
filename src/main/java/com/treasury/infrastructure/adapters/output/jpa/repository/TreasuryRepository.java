package com.treasury.infrastructure.adapters.output.jpa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.treasury.infrastructure.adapters.output.jpa.entity.TreasuryEntity;

@Repository
public interface TreasuryRepository extends JpaRepository<TreasuryEntity, Long> {
    Optional<TreasuryEntity> findByAccountNumber(String accountNumber);
    boolean existsByAccountNumber(String accountNumber);

    @Modifying
    @Query("UPDATE TreasuryEntity t SET t.status = false WHERE t.id = :id")
    void inactivateById(@Param("id") Long id);

    @Modifying
    @Query("UPDATE TreasuryEntity t SET t.status = true WHERE t.id = :id")
    void activateById(@Param("id") Long id);
}
