package com.treasury.infrastructure.adapters.output.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import java.time.Instant;

@Entity @Table(name="outbox_events",uniqueConstraints=@UniqueConstraint(name="uk_outbox_event_id",columnNames="event_id"))
@Getter @Setter @NoArgsConstructor
public class OutboxEventEntity {
    public enum Status { PENDING, PUBLISHED, FAILED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="event_id",nullable=false,length=80) private String eventId;
    @Column(name="aggregate_type",nullable=false,length=50) private String aggregateType;
    @Column(name="aggregate_id",nullable=false) private Long aggregateId;
    @Column(name="event_type",nullable=false,length=80) private String eventType;
    @Column(nullable=false,columnDefinition="TEXT") private String payload;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="published_at") private Instant publishedAt;
    @Column(name="attempts",nullable=false) private int attempts;
    @Column(name="last_error",length=500) private String lastError;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status=Status.PENDING;
    @Column(name="next_attempt_at",nullable=false) private Instant nextAttemptAt;
    @TenantId @Column(name="tenant_id",nullable=false,length=80) private String tenantId;
    @PrePersist void created(){createdAt=Instant.now();if(nextAttemptAt==null)nextAttemptAt=createdAt;if(status==null)status=Status.PENDING;}
}
