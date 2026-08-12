package com.treasury.infrastructure.adapters.output.jpa.adapter;

import com.treasury.domain.model.command.TreasuryCommands.*;
import com.treasury.application.output.IPaymentVoucherCommandPersistencePort;
import com.treasury.application.output.IPaymentVoucherQueryPersistencePort;
import com.treasury.domain.model.PaymentVoucher;
import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentVoucherEntity;
import com.treasury.infrastructure.adapters.output.jpa.mapper.IPaymentVoucherPersistenceMapper;
import com.treasury.infrastructure.adapters.output.jpa.repository.IPaymentVoucherRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component @RequiredArgsConstructor
public class PaymentVoucherJpaAdapter implements IPaymentVoucherCommandPersistencePort, IPaymentVoucherQueryPersistencePort {
    private final IPaymentVoucherRepository repository;
    private final IPaymentVoucherPersistenceMapper mapper;
    @Override public PaymentVoucher save(PaymentVoucher value){return mapper.toDomain(repository.save(mapper.toEntity(value)));}
    @Override public Optional<PaymentVoucher> find(Long id,String enterpriseId){return repository.findByIdAndEnterpriseId(id,enterpriseId).map(mapper::toDomain);}
    @Override public Optional<PaymentVoucher> findById(Long id){return repository.findById(id).map(mapper::toDomain);}
    @Override public Optional<PaymentVoucher> findByIdempotencyKey(String key,String enterpriseId){return repository.findByIdempotencyKeyAndEnterpriseId(key,enterpriseId).map(mapper::toDomain);}
    @Override public PageResult<PaymentVoucher> search(VoucherFilter f){Specification<PaymentVoucherEntity> spec=(r,q,c)->c.equal(r.get("enterpriseId"),f.enterpriseId());if(f.voucherNumber()!=null&&!f.voucherNumber().isBlank())spec=spec.and((r,q,c)->c.like(c.lower(r.get("voucherNumber")),"%"+f.voucherNumber().toLowerCase()+"%"));if(f.status()!=null)spec=spec.and((r,q,c)->c.equal(r.get("status"),f.status()));if(f.from()!=null)spec=spec.and((r,q,c)->c.greaterThanOrEqualTo(r.get("issueDate"),f.from()));if(f.to()!=null)spec=spec.and((r,q,c)->c.lessThanOrEqualTo(r.get("issueDate"),f.to()));if(f.paymentMethodId()!=null)spec=spec.and((r,q,c)->c.equal(r.get("paymentMethodId"),f.paymentMethodId()));if(f.bankAccountId()!=null)spec=spec.and((r,q,c)->c.equal(r.get("bankAccountId"),f.bankAccountId()));if(f.min()!=null)spec=spec.and((r,q,c)->c.greaterThanOrEqualTo(r.get("total"),f.min()));if(f.max()!=null)spec=spec.and((r,q,c)->c.lessThanOrEqualTo(r.get("total"),f.max()));if(f.supplierId()!=null||f.invoiceId()!=null)spec=spec.and((r,q,c)->{var d=r.join("details",JoinType.INNER);q.distinct(true);return f.supplierId()!=null?c.equal(d.get("supplierId"),f.supplierId()):c.equal(d.get("invoiceId"),f.invoiceId());});Sort sort=parseSort(f.sort());Page<PaymentVoucher> page=repository.findAll(spec,PageRequest.of(f.page(),f.size(),sort)).map(mapper::toDomain);return new PageResult<>(page.getContent(),page.getTotalElements(),page.getTotalPages(),page.getNumber(),page.getSize());}
    @Override public void delete(PaymentVoucher value){repository.deleteById(value.getId());}
    private Sort parseSort(String value){if(value==null||value.isBlank())return Sort.by(Sort.Direction.DESC,"id");String[] p=value.split(",");return Sort.by(p.length>1&&"asc".equalsIgnoreCase(p[1])?Sort.Direction.ASC:Sort.Direction.DESC,p[0]);}
}
