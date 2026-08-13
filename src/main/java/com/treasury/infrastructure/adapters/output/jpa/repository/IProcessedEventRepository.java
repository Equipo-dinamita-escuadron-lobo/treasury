package com.treasury.infrastructure.adapters.output.jpa.repository;

import com.treasury.infrastructure.adapters.output.jpa.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IProcessedEventRepository extends JpaRepository<ProcessedEventEntity,Long> { boolean existsByEventId(String eventId); }
