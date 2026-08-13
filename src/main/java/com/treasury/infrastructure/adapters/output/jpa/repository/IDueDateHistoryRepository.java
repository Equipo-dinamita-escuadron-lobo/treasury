package com.treasury.infrastructure.adapters.output.jpa.repository;

import com.treasury.infrastructure.adapters.output.jpa.entity.DueDateHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IDueDateHistoryRepository extends JpaRepository<DueDateHistoryEntity,Long> { List<DueDateHistoryEntity> findByInvoiceIdOrderByChangedAtDesc(Long invoiceId); }
