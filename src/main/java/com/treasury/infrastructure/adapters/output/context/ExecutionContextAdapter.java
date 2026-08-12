package com.treasury.infrastructure.adapters.output.context;

import com.treasury.application.output.IExecutionContextPort;
import com.treasury.infrastructure.adapters.output.multitenancy.utils.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class ExecutionContextAdapter implements IExecutionContextPort {
    @Override public String tenantId(){return Optional.ofNullable(TenantContext.getTenantId()).orElse("system");}
    @Override public String username(){Authentication auth=SecurityContextHolder.getContext().getAuthentication();return auth==null?"system":auth.getName();}
    @Override public void runAsTenant(String tenantId,Runnable action){String previous=TenantContext.getTenantId();TenantContext.setTenantId(tenantId);try{action.run();}finally{if(previous==null)TenantContext.clear();else TenantContext.setTenantId(previous);}}
}
