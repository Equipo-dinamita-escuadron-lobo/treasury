package com.treasury.application.service;

import com.treasury.application.output.IExecutionContextPort;
import com.treasury.application.output.IPayableWriteOffPersistencePort;
import com.treasury.application.output.ISupplierInvoiceProviderPort;
import com.treasury.application.output.ITreasuryAuditPersistencePort;
import com.treasury.application.output.ITreasuryEventPublisher;
import com.treasury.domain.model.PayableWriteOff;
import com.treasury.domain.model.WriteOffStatus;
import com.treasury.domain.model.command.TreasuryCommands.AccountingResult;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayableWriteOffServiceTest {
    @Mock IPayableWriteOffPersistencePort writeOffs;
    @Mock ISupplierInvoiceProviderPort invoices;
    @Mock ITreasuryEventPublisher events;
    @Mock ITreasuryAuditPersistencePort audit;
    @Mock IExecutionContextPort context;
    PayableWriteOffService service;

    @BeforeEach void setUp() {
        service = new PayableWriteOffService(writeOffs, invoices, events, audit, context);
    }

    @Test void voidAccountingAckIsAuditedWithoutApplyingBalancesAgain() {
        PayableWriteOff writeOff = new PayableWriteOff();
        writeOff.setId(7L);
        writeOff.setStatus(WriteOffStatus.VOIDED);
        when(audit.wasProcessed("result-void")).thenReturn(false);
        when(writeOffs.find(7L)).thenReturn(Optional.of(writeOff));

        service.applyAccountingResult(new AccountingResult(
                "result-void", "PAYABLE_WRITEOFF", 7L, true, 19L, null, "tenant"));

        verify(audit).markProcessed("result-void", "tenant");
        verifyNoInteractions(invoices);
        verify(writeOffs, never()).save(any());
    }
}
