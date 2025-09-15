package com.treasury.infrastructure.adapters.output.multitenancy.interceptor;

import com.treasury.infrastructure.adapters.output.multitenancy.utils.TenantContext;
import com.treasury.infrastructure.adapters.output.security.IJwtUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;

@Component
public class TenantInterceptor implements WebRequestInterceptor {

    @Autowired
    private IJwtUtils jwtUtils;

    /**
     * Este método se llama antes de que se llame al controlador, y establece el
     * identificador
     * de inquilino desde el JWT en el TenantContext.
     *
     * @param request La solicitud web
     * @throws Exception Si no se pudo establecer el identificador de inquilino
     *                   desde el JWT
     */
    @Override
    public void preHandle(WebRequest request) throws Exception {
        TenantContext.setTenantId(jwtUtils.getId());
    }

    /**
     * Este metodo se llama despu s de que se llama al controlador. Borra el
     * identificador de inquilino del TenantContext.
     *  */
    @Override
    public void postHandle(WebRequest request, ModelMap model) throws Exception {
        TenantContext.clear();
    }

    /**
     * Este metodo se llama despu s de que se llama al controlador y
     * despu s de que se llama al m todo postHandle. No hace nada en este
     * caso, pero se declara para implementar la interfaz WebRequestInterceptor.
     *
     * @param request La solicitud web
     * @param ex      La excepcion lanzada por el controlador, si es que se
     *               lanza, o null si no se lanz  ninguna excepci n
     * @throws Exception Si se produce un error inesperado
     */
    @Override
    public void afterCompletion(WebRequest request, Exception ex) throws Exception {

    }
}
