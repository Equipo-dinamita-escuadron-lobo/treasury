package com.treasury.infrastructure;

import com.treasury.application.input.*;
import com.treasury.application.output.IPaymentMethodProviderPort;
import com.treasury.domain.model.command.TreasuryCommands.*;
import com.treasury.domain.model.PaymentMethodData;
import com.treasury.domain.model.PaymentVoucherStatus;
import com.treasury.infrastructure.adapters.output.multitenancy.utils.TenantContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest @ActiveProfiles("test")
class HexagonalFlowIntegrationTest {
    @Autowired IInvoiceSynchronizationUseCase synchronization;
    @Autowired IPayableQueryUseCase payables;
    @Autowired IPaymentVoucherCommandUseCase voucherCommands;
    @Autowired IPaymentVoucherQueryUseCase voucherQueries;
    @MockBean IPaymentMethodProviderPort paymentMethods;
    private final String tenant="flow-tenant";
    @BeforeEach void tenant(){TenantContext.setTenantId(tenant);when(paymentMethods.findActive(17L,"enterprise-flow")).thenReturn(java.util.Optional.of(new PaymentMethodData(17L,false)));}
    @AfterEach void clear(){TenantContext.clear();}

    @Test void purchaseReplicaVoucherReservationAndAccountingAckCrossPortsAndJpaAdapters(){
        synchronization.synchronize(new PurchaseInvoiceEvent("purchase-flow-event","PURCHASE_INVOICE_CREATED",7001L,"FC-7001","enterprise-flow",501L,new BigDecimal("100.00"),BigDecimal.ZERO,new BigDecimal("100.00"),LocalDate.now(),LocalDate.now().plusDays(10),2205L,"2205",true,tenant));
        var invoice=payables.pending("enterprise-flow",501L).get(0);
        var voucher=voucherCommands.create(new Voucher("enterprise-flow",LocalDate.now(),17L,null,"integración",List.of(new Detail(501L,invoice.getId(),new BigDecimal("30.00")))));
        voucher=voucherCommands.post(voucher.getId(),"enterprise-flow","flow-idempotency-key");
        assertThat(voucher.getStatus()).isEqualTo(PaymentVoucherStatus.POSTING);
        assertThat(payables.find(invoice.getId(),"enterprise-flow").getReservedAmount()).isEqualByComparingTo("30.00");
        voucherCommands.applyAccountingResult(new AccountingResult("accounting-flow-event","PAYMENT_VOUCHER",voucher.getId(),true,9901L,null,tenant));
        var posted=voucherQueries.find(voucher.getId(),"enterprise-flow");var paid=payables.find(invoice.getId(),"enterprise-flow");
        assertThat(posted.getStatus()).isEqualTo(PaymentVoucherStatus.POSTED);assertThat(posted.getAccountingEntryId()).isEqualTo(9901L);
        assertThat(paid.getPendingAmount()).isEqualByComparingTo("70.00");assertThat(paid.getReservedAmount()).isZero();
    }
}
