package com.treasury.infrastructure.adapters.output.jpa.repository;

import com.treasury.infrastructure.adapters.output.jpa.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IOutboxEventRepository extends JpaRepository<OutboxEventEntity,Long> {
    List<OutboxEventEntity> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
    @org.springframework.data.jpa.repository.Query(value="select * from outbox_events where status in ('PENDING','FAILED') and attempts < 5 and next_attempt_at <= now() order by created_at limit 100",nativeQuery=true)
    List<OutboxEventEntity> findUnpublishedAcrossTenants();
}
