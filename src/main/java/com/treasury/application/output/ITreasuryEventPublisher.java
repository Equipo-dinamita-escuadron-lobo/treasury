package com.treasury.application.output;

import com.treasury.domain.model.TreasuryEvent;

public interface ITreasuryEventPublisher {
    void enqueue(TreasuryEvent event);
}
