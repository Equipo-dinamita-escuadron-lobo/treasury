package com.treasury.application.input;

import com.treasury.domain.model.PayableWriteOff;
import java.util.List;

public interface IPayableWriteOffQueryUseCase {
    PayableWriteOff find(Long id);
    List<PayableWriteOff> list(String enterpriseId);
}
