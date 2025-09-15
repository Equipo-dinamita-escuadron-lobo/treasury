package com.treasury.infrastructure.adapters.output.exception.handler;

public class BusinessRuleException extends BaseException {
    public BusinessRuleException(int status, String message) {
        super(status, message);
    }
}
