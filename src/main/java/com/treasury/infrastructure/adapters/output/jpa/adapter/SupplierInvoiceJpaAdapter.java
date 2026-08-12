package com.treasury.infrastructure.adapters.output.jpa.adapter;

import com.treasury.application.output.ISupplierInvoiceProviderPort;
import com.treasury.domain.model.SupplierInvoiceReplica;
import com.treasury.infrastructure.adapters.output.jpa.entity.SupplierInvoiceReplicaEntity;
import com.treasury.infrastructure.adapters.output.jpa.mapper.ISupplierInvoicePersistenceMapper;
import com.treasury.infrastructure.adapters.output.jpa.repository.ISupplierInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.*;

@Component @RequiredArgsConstructor
public class SupplierInvoiceJpaAdapter implements ISupplierInvoiceProviderPort {
    private final ISupplierInvoiceRepository repository;
    private final ISupplierInvoicePersistenceMapper mapper;
    @Override public SupplierInvoiceReplica save(SupplierInvoiceReplica value){return mapper.toDomain(repository.save(mapper.toEntity(value)));}
    @Override public Optional<SupplierInvoiceReplica> findById(Long id){return repository.findById(id).map(mapper::toDomain);}
    @Override public Optional<SupplierInvoiceReplica> findLocked(Long id,String enterpriseId){return repository.findLocked(id,enterpriseId).map(mapper::toDomain);}
    @Override public Optional<SupplierInvoiceReplica> findBySource(Long sourceId,String enterpriseId){return repository.findBySourceInvoiceIdAndEnterpriseId(sourceId,enterpriseId).map(mapper::toDomain);}
    @Override public List<SupplierInvoiceReplica> findPending(String enterpriseId,Long supplierId){var list=supplierId==null?repository.findByEnterpriseIdAndPendingAmountGreaterThanAndActiveTrue(enterpriseId,BigDecimal.ZERO):repository.findByEnterpriseIdAndSupplierIdAndPendingAmountGreaterThanAndActiveTrue(enterpriseId,supplierId,BigDecimal.ZERO);return mapper.toDomainList(list);}
    @Override public List<SupplierInvoiceReplica> findForStatement(String enterpriseId,Long supplierId,java.time.LocalDate from,java.time.LocalDate to,String reference,Boolean active){return mapper.toDomainList(repository.findByEnterpriseId(enterpriseId)).stream().filter(i->supplierId==null||supplierId.equals(i.getSupplierId())).filter(i->from==null||!i.getIssueDate().isBefore(from)).filter(i->to==null||!i.getIssueDate().isAfter(to)).filter(i->reference==null||reference.isBlank()||i.getReference().toLowerCase().contains(reference.toLowerCase())).filter(i->active==null||active==i.isActive()).toList();}
}
