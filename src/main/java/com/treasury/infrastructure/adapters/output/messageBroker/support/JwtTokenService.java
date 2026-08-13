package com.treasury.infrastructure.adapters.output.messageBroker.support;

import com.treasury.infrastructure.adapters.output.security.IJwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JwtTokenService {
    private static final ThreadLocal<String> rabbitJwtToken = new ThreadLocal<>();
    private static final ThreadLocal<String> rabbitTenantId = new ThreadLocal<>();

    @Autowired
    private IJwtUtils jwtUtils;

    public void setRabbitJwtToken(String token) {
        rabbitJwtToken.set(token);
    }

    public void setRabbitTenantId(String tenantId) {
        rabbitTenantId.set(tenantId);
    }

    public String getToken() {
        String token = rabbitJwtToken.get();
        if (token != null) {
            return token.startsWith("Bearer ") ? token.substring(7) : token;
        }
        return jwtUtils.getToken();
    }

    public String getTenantId() {
        String tenantId = rabbitTenantId.get();
        if (tenantId != null) {
            return tenantId;
        }
        return jwtUtils.getId();
    }

    public void clearRabbitContext() {
        rabbitJwtToken.remove();
        rabbitTenantId.remove();
    }
}
