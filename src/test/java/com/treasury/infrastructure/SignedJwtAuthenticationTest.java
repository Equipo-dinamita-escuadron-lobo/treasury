package com.treasury.infrastructure;

import com.treasury.application.input.IPaymentVoucherCommandUseCase;
import com.treasury.application.input.IPaymentVoucherQueryUseCase;
import com.treasury.domain.model.command.TreasuryCommands.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@ContextConfiguration(classes={com.treasury.TreasuryApplication.class,SignedJwtAuthenticationTest.JwtTestConfiguration.class})
class SignedJwtAuthenticationTest {
    private static final KeyPair KEYS=keys();
    @Autowired MockMvc mvc;
    @MockBean IPaymentVoucherCommandUseCase commands;
    @MockBean IPaymentVoucherQueryUseCase queries;

    @Test void validSignedUserClientTokenIsAuthenticatedAndAuthorized()throws Exception{when(queries.search(any())).thenReturn(new PageResult<>(java.util.List.of(),0,0,0,20));mvc.perform(get("/api/treasury/payment-vouchers").param("enterpriseId","ent").header("Authorization","Bearer "+token("user_client"))).andExpect(status().isOk());}
    @Test void validSignedTokenWithoutOperationalRoleIsForbidden()throws Exception{mvc.perform(get("/api/treasury/payment-vouchers").param("enterpriseId","ent").header("Authorization","Bearer "+token("Invitado"))).andExpect(status().isForbidden());}
    @Test void tamperedTokenIsRejected()throws Exception{String token=token("user_client");mvc.perform(get("/api/treasury/payment-vouchers").param("enterpriseId","ent").header("Authorization","Bearer "+token.substring(0,token.length()-2)+"xx")).andExpect(status().isUnauthorized());}

    private static String token(String role)throws Exception{long now=Instant.now().getEpochSecond();String header=b64("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");String claims=b64("{\"sub\":\"user-1\",\"preferred_username\":\"student\",\"iat\":"+now+",\"exp\":"+(now+600)+",\"resource_access\":{\"microservices_client\":{\"roles\":[\""+role+"\"]}}}");String content=header+"."+claims;Signature signature=Signature.getInstance("SHA256withRSA");signature.initSign(KEYS.getPrivate());signature.update(content.getBytes(StandardCharsets.US_ASCII));return content+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());}
    private static String b64(String value){return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));}
    private static KeyPair keys(){try{KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);return generator.generateKeyPair();}catch(Exception ex){throw new IllegalStateException(ex);}}
    @TestConfiguration static class JwtTestConfiguration{@Bean @Primary JwtDecoder jwtDecoder(){return NimbusJwtDecoder.withPublicKey((RSAPublicKey)KEYS.getPublic()).build();}}
}
