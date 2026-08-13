package com.treasury.infrastructure.adapters.output.jpa.adapter;

import com.treasury.application.output.IPayableWriteOffPersistencePort;
import com.treasury.domain.model.PayableWriteOff;
import com.treasury.infrastructure.adapters.output.jpa.entity.PayableWriteOffEntity;
import com.treasury.infrastructure.adapters.output.jpa.mapper.IPayableWriteOffPersistenceMapper;
import com.treasury.infrastructure.adapters.output.jpa.repository.IPayableWriteOffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;

@Component @RequiredArgsConstructor
public class PayableWriteOffJpaAdapter implements IPayableWriteOffPersistencePort {
    private final IPayableWriteOffRepository repository;
    private final IPayableWriteOffPersistenceMapper mapper;
    @Override public PayableWriteOff save(PayableWriteOff value){return mapper.toDomain(repository.save(mapper.toEntity(value)));}
    @Override public Optional<PayableWriteOff> find(Long id){return repository.findById(id).map(mapper::toDomain);}
    @Override public List<PayableWriteOff> findByEnterprise(String enterpriseId){return mapper.toDomainList(repository.findByEnterpriseId(enterpriseId));}
}
