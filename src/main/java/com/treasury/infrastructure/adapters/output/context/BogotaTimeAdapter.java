package com.treasury.infrastructure.adapters.output.context;

import com.treasury.application.output.ITimeProviderPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.*;

@Component
public class BogotaTimeAdapter implements ITimeProviderPort {
    @Value("${treasury.scheduler.timezone:America/Bogota}") private String timezone;
    @Override public LocalDate today(){return LocalDate.now(ZoneId.of(timezone));}
}
