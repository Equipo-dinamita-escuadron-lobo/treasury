package com.treasury.infrastructure.adapters.input.scheduler;

import com.treasury.application.input.IPaymentScheduleExecutionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.*;

@Component @RequiredArgsConstructor
public class PaymentScheduleRunner {
    private final IPaymentScheduleExecutionUseCase useCase;
    @Value("${treasury.scheduler.timezone:America/Bogota}") private String timezone;
    @Scheduled(fixedDelayString="${treasury.scheduler.payment-delay-ms:60000}")
    public void executeDue(){useCase.recoverAbandoned(Instant.now().minus(Duration.ofMinutes(10)));useCase.executeDue(LocalDate.now(ZoneId.of(timezone)));}
}
