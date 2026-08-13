package com.treasury.application.output;

import com.treasury.domain.model.PayableWriteOff;
import java.util.List;
import java.util.Optional;

public interface IPayableWriteOffPersistencePort {
    PayableWriteOff save(PayableWriteOff writeOff);
    Optional<PayableWriteOff> find(Long id);
    List<PayableWriteOff> findByEnterprise(String enterpriseId);
}
