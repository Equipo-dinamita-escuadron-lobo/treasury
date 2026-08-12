package com.treasury.infrastructure.adapters.output.jpa.adapter;

import com.treasury.domain.model.command.TreasuryCommands.*;
import com.treasury.application.output.IPaymentVoucherCommandPersistencePort;
import com.treasury.application.output.IPaymentVoucherQueryPersistencePort;
import com.treasury.domain.model.PaymentVoucher;
import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentVoucherDetailEntity;
import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentVoucherEntity;
import com.treasury.infrastructure.adapters.output.jpa.mapper.IPaymentVoucherPersistenceMapper;
import com.treasury.infrastructure.adapters.output.jpa.repository.IPaymentVoucherRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component @RequiredArgsConstructor
public class PaymentVoucherJpaAdapter implements IPaymentVoucherCommandPersistencePort, IPaymentVoucherQueryPersistencePort {
    private final IPaymentVoucherRepository repository;
    private final IPaymentVoucherPersistenceMapper mapper;

    /** On update: flush deletes before inserts so uk_voucher_invoice allows same-obligation edits (L-01/B-25). */
    @Override
    public PaymentVoucher save(PaymentVoucher value) {
        PaymentVoucherEntity incoming = mapper.toEntity(value);
        if (incoming.getId() == null) {
            return mapper.toDomain(repository.save(incoming));
        }

        PaymentVoucherEntity managed = repository.findById(incoming.getId())
                .orElseThrow(() -> new IllegalStateException("Comprobante no encontrado: " + incoming.getId()));

        copyHeader(incoming, managed);

        List<PaymentVoucherDetailEntity> replacement = new ArrayList<>(incoming.getDetails());
        managed.getDetails().clear();
        repository.flush();

        for (PaymentVoucherDetailEntity detail : replacement) {
            detail.setId(null);
            detail.setPaymentVoucher(managed);
            managed.getDetails().add(detail);
        }
        return mapper.toDomain(repository.save(managed));
    }

    private static void copyHeader(PaymentVoucherEntity from, PaymentVoucherEntity to) {
        to.setVoucherNumber(from.getVoucherNumber());
        to.setEnterpriseId(from.getEnterpriseId());
        to.setIssueDate(from.getIssueDate());
        to.setStatus(from.getStatus());
        to.setPaymentMethodId(from.getPaymentMethodId());
        to.setBankAccountId(from.getBankAccountId());
        to.setTotal(from.getTotal());
        to.setObservations(from.getObservations());
        to.setIdempotencyKey(from.getIdempotencyKey());
        to.setAccountingEntryId(from.getAccountingEntryId());
        to.setFailureReason(from.getFailureReason());
        to.setVoidReason(from.getVoidReason());
        to.setTenantId(from.getTenantId());
    }

    @Override public Optional<PaymentVoucher> find(Long id,String enterpriseId){return repository.findByIdAndEnterpriseId(id,enterpriseId).map(mapper::toDomain);}
    @Override public Optional<PaymentVoucher> findById(Long id){return repository.findById(id).map(mapper::toDomain);}
    @Override public Optional<PaymentVoucher> findByIdempotencyKey(String key,String enterpriseId){return repository.findByIdempotencyKeyAndEnterpriseId(key,enterpriseId).map(mapper::toDomain);}
    @Override public PageResult<PaymentVoucher> search(VoucherFilter f){Specification<PaymentVoucherEntity> spec=(r,q,c)->c.equal(r.get("enterpriseId"),f.enterpriseId());if(f.voucherNumber()!=null&&!f.voucherNumber().isBlank())spec=spec.and((r,q,c)->c.like(c.lower(r.get("voucherNumber")),"%"+f.voucherNumber().toLowerCase()+"%"));if(f.status()!=null)spec=spec.and((r,q,c)->c.equal(r.get("status"),f.status()));if(f.from()!=null)spec=spec.and((r,q,c)->c.greaterThanOrEqualTo(r.get("issueDate"),f.from()));if(f.to()!=null)spec=spec.and((r,q,c)->c.lessThanOrEqualTo(r.get("issueDate"),f.to()));if(f.paymentMethodId()!=null)spec=spec.and((r,q,c)->c.equal(r.get("paymentMethodId"),f.paymentMethodId()));if(f.bankAccountId()!=null)spec=spec.and((r,q,c)->c.equal(r.get("bankAccountId"),f.bankAccountId()));if(f.min()!=null)spec=spec.and((r,q,c)->c.greaterThanOrEqualTo(r.get("total"),f.min()));if(f.max()!=null)spec=spec.and((r,q,c)->c.lessThanOrEqualTo(r.get("total"),f.max()));if(f.supplierId()!=null||f.invoiceId()!=null)spec=spec.and((r,q,c)->{var d=r.join("details",JoinType.INNER);q.distinct(true);return f.supplierId()!=null?c.equal(d.get("supplierId"),f.supplierId()):c.equal(d.get("invoiceId"),f.invoiceId());});Sort sort=parseSort(f.sort());Page<PaymentVoucher> page=repository.findAll(spec,PageRequest.of(f.page(),f.size(),sort)).map(mapper::toDomain);return new PageResult<>(page.getContent(),page.getTotalElements(),page.getTotalPages(),page.getNumber(),page.getSize());}
    @Override public void delete(PaymentVoucher value){repository.deleteById(value.getId());}
    private Sort parseSort(String value){if(value==null||value.isBlank())return Sort.by(Sort.Direction.DESC,"id");String[] p=value.split(",");return Sort.by(p.length>1&&"asc".equalsIgnoreCase(p[1])?Sort.Direction.ASC:Sort.Direction.DESC,p[0]);}
}
