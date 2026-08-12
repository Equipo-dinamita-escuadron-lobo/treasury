package com.treasury.application.output;

public interface IExecutionContextPort {
    String tenantId();
    String username();
    void runAsTenant(String tenantId, Runnable action);
}
