package com.treasury.infrastructure.adapters.output.jpa.adapter;

import com.treasury.domain.model.command.TreasuryCommands.ScheduleFilter;
import com.treasury.application.output.IPaymentSchedulePersistencePort;
import com.treasury.domain.model.DuePaymentSchedule;
import com.treasury.domain.model.PaymentSchedule;
import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentScheduleEntity;
import com.treasury.infrastructure.adapters.output.jpa.mapper.IPaymentSchedulePersistenceMapper;
import com.treasury.infrastructure.adapters.output.jpa.repository.IPaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.Instant;
import java.util.*;

@Component @RequiredArgsConstructor
public class PaymentScheduleJpaAdapter implements IPaymentSchedulePersistencePort {
    private final IPaymentScheduleRepository repository;
    private final IPaymentSchedulePersistenceMapper mapper;
    @Override public PaymentSchedule save(PaymentSchedule value){return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(value)));}
    @Override public Optional<PaymentSchedule> find(Long id){return repository.findById(id).map(mapper::toDomain);}
    @Override public Optional<PaymentSchedule> findLocked(Long id){return repository.findLocked(id).map(mapper::toDomain);}
    @Override public Optional<PaymentSchedule> findByVoucherId(Long voucherId){return repository.findByVoucherId(voucherId).map(mapper::toDomain);}
    @Override public List<PaymentSchedule> search(ScheduleFilter f){return repository.findByEnterpriseId(f.enterpriseId()).stream().filter(s->f.status()==null||s.getStatus()==f.status()).filter(s->f.from()==null||!s.getExecutionDate().isBefore(f.from())).filter(s->f.to()==null||!s.getExecutionDate().isAfter(f.to())).map(mapper::toDomain).toList();}
    @Override public List<DuePaymentSchedule> findDue(LocalDate date){return repository.findDueClaims(date).stream().map(c->new DuePaymentSchedule(c.getId(),c.getTenantId())).toList();}
    @Override public List<DuePaymentSchedule> findAbandoned(Instant before){return repository.findAbandonedClaims(before).stream().map(c->new DuePaymentSchedule(c.getId(),c.getTenantId())).toList();}
    @Override public void delete(PaymentSchedule value){repository.deleteById(value.getId());}
}
