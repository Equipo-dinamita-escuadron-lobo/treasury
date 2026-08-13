package com.treasury.application.output;

public interface ITransactionRunnerPort {
    void run(Runnable action);
}
