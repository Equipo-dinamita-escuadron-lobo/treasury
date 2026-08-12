package com.treasury.infrastructure.adapters.output.jpa.repository;

import com.treasury.infrastructure.adapters.output.jpa.entity.SupplierInvoiceReplicaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ISupplierInvoiceRepository extends JpaRepository<SupplierInvoiceReplicaEntity, Long>, JpaSpecificationExecutor<SupplierInvoiceReplicaEntity> {
    List<SupplierInvoiceReplicaEntity> findByEnterpriseId(String enterpriseId);
    Optional<SupplierInvoiceReplicaEntity> findBySourceInvoiceIdAndEnterpriseId(Long sourceId, String enterpriseId);
    List<SupplierInvoiceReplicaEntity> findByEnterpriseIdAndSupplierIdAndPendingAmountGreaterThanAndActiveTrue(String enterpriseId, Long supplierId, BigDecimal zero);
    List<SupplierInvoiceReplicaEntity> findByEnterpriseIdAndPendingAmountGreaterThanAndActiveTrue(String enterpriseId, BigDecimal zero);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from SupplierInvoiceReplicaEntity i where i.id = :id and i.enterpriseId = :enterpriseId")
    Optional<SupplierInvoiceReplicaEntity> findLocked(@Param("id") Long id, @Param("enterpriseId") String enterpriseId);
}
