package com.treasury.infrastructure.adapters.output.transaction;

import com.treasury.application.output.ITransactionRunnerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component @RequiredArgsConstructor
public class SpringTransactionRunnerAdapter implements ITransactionRunnerPort {
    private final TransactionTemplate transactions;
    @Override public void run(Runnable action){transactions.executeWithoutResult(status->action.run());}
}
