package com.treasury.infrastructure.adapters.output.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import java.time.Instant;

@Entity @Table(name="processed_events",uniqueConstraints=@UniqueConstraint(name="uk_processed_event_id",columnNames="event_id"))
@Getter @Setter @NoArgsConstructor
public class ProcessedEventEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="event_id",nullable=false,length=80) private String eventId;
    @Column(name="processed_at",nullable=false) private Instant processedAt=Instant.now();
    @TenantId @Column(name="tenant_id",nullable=false,length=80) private String tenantId;
}
