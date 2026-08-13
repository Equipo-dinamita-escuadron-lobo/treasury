package com.treasury.domain.exception;

public class TreasuryException extends RuntimeException {
    public enum Type { BAD_REQUEST, CONFLICT, NOT_FOUND, DEPENDENCY_UNAVAILABLE }
    private final Type type;

    public TreasuryException(Type type, String message) {
        super(message);
        this.type = type;
    }

    public Type getType() { return type; }
}
