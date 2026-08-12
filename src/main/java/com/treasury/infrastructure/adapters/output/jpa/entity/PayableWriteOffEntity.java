package com.treasury.infrastructure.adapters.output.jpa.entity;

import com.treasury.domain.model.WriteOffStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name="payable_write_offs")
@Getter @Setter @NoArgsConstructor
public class PayableWriteOffEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="enterprise_id",nullable=false,length=80) private String enterpriseId;
    @Column(nullable=false,length=500) private String reason;
    @Column(name="counterpart_account_id",nullable=false) private Long counterpartAccountId;
    @Column(name="counterpart_account_code",nullable=false,length=30) private String counterpartAccountCode;
    @Column(nullable=false,precision=19,scale=2) private BigDecimal total=BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private WriteOffStatus status=WriteOffStatus.DRAFT;
    @Column(name="accounting_entry_id") private Long accountingEntryId;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Version private long version;
    @TenantId @Column(name="tenant_id",nullable=false,length=80) private String tenantId;
    @OneToMany(mappedBy="writeOff",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.EAGER) private List<PayableWriteOffDetailEntity> details=new ArrayList<>();
    @PrePersist void created(){createdAt=updatedAt=Instant.now();}
    @PreUpdate void updated(){updatedAt=Instant.now();}
}
