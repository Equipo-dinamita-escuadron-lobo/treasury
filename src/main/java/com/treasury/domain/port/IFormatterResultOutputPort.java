package com.treasury.domain.port;

public interface IFormatterResultOutputPort {
    void returnResponseError(int status, String message);
}
