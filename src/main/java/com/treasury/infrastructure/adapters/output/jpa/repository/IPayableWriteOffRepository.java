package com.treasury.infrastructure.adapters.output.jpa.repository;

import com.treasury.infrastructure.adapters.output.jpa.entity.PayableWriteOffEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IPayableWriteOffRepository extends JpaRepository<PayableWriteOffEntity,Long> {
    List<PayableWriteOffEntity> findByEnterpriseId(String enterpriseId);
}
