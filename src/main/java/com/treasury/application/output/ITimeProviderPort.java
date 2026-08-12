package com.treasury.application.output;

import java.time.LocalDate;

public interface ITimeProviderPort {
    LocalDate today();
}
