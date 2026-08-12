package com.treasury.infrastructure;

import com.treasury.application.input.IPaymentVoucherCommandUseCase;
import com.treasury.application.input.IPaymentVoucherQueryUseCase;
import com.treasury.domain.model.command.TreasuryCommands.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentVoucherSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean IPaymentVoucherCommandUseCase commands;
    @MockBean IPaymentVoucherQueryUseCase queries;

    @Test void anonymousUserIsRejected() throws Exception {
        mvc.perform(get("/api/treasury/payment-vouchers").param("enterpriseId", "ent")).andExpect(status().isUnauthorized());
    }

    @Test void studentIsAccepted() throws Exception { assertOperationalRole("Estudiante"); }
    @Test void teacherIsAccepted() throws Exception { assertOperationalRole("Profesor"); }
    @Test void administratorIsAccepted() throws Exception { assertOperationalRole("Administrador"); }

    private void assertOperationalRole(String role) throws Exception {
        when(queries.search(any())).thenReturn(new PageResult<>(java.util.List.of(),0,0,0,20));
        mvc.perform(get("/api/treasury/payment-vouchers").param("enterpriseId", "ent")
                .with(jwt().jwt(token -> token.claim("tenantId", "tenant").subject("tester"))
                        .authorities(new SimpleGrantedAuthority("ROLE_" + role)))).andExpect(status().isOk());
    }
}
