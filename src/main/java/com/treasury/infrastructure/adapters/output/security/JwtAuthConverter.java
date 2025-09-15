package com.treasury.infrastructure.adapters.output.security;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken>, IJwtUtils {

    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Value("${jwt.auth.converter.principle-attribute}")
    private String principleAtrribute;

    @Value("${jwt.auth.converter.resource-id}")
    private String resourceId;

    Jwt jwtToken;

    /**
     * Convierte un JWT en un {@link AbstractAuthenticationToken} que se puede
     * utilizar
     * para autenticación.
     *
     * @param jwt el JWT a convertir
     * @return el {@link AbstractAuthenticationToken} correspondiente
     */
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        Collection<GrantedAuthority> authorities = Stream
                .concat(jwtGrantedAuthoritiesConverter.convert(jwt).stream(), extractResourceRoles(jwt).stream())
                .toList();

        this.jwtToken = jwt;

        return new JwtAuthenticationToken(jwt, authorities, getPrincipleName(jwt));

    }

    /**
     * Devuelve el nombre del usuario autenticado en el JWT.
     *
     * Por defecto, se utiliza el claim "sub" del JWT, pero se puede
     * configurar un claim diferente estableciendo la propiedad
     * "jwt.auth.converter.principle-attribute".
     *
     * @param jwt el JWT del que se extrae el nombre del usuario
     * @return el nombre del usuario autenticado
     */
    private String getPrincipleName(Jwt jwt) {
        String claimName = JwtClaimNames.SUB;

        if (principleAtrribute != null) {
            claimName = principleAtrribute;
        }

        return jwt.getClaim(claimName);
    }

    /**
     * Extrae roles del reclamo "resource_access" del JWT para el ID de recurso
     * configurado.
     * Convierte cada rol en un {@link SimpleGrantedAuthority} con el prefijo
     * "ROLE_".
     *
     * Si el reclamo "resource_access" o el ID de recurso específico o sus roles no
     * están presentes,
     * devuelve una colección vacía.
     *
     * @param jwt el JWT desde el cual extraer los roles
     * @return una colección de {@link GrantedAuthority} que representan los roles
     */
    @SuppressWarnings("unchecked")
    private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {
        Map<String, Object> resourceAccess;
        Map<String, Object> resource;
        Collection<String> resourceRoles;

        if (jwt.getClaim("resource_access") == null) {
            return List.of();
        }

        resourceAccess = jwt.getClaim("resource_access");

        if (resourceAccess.get(resourceId) == null) {
            return List.of();
        }

        resource = (Map<String, Object>) resourceAccess.get(resourceId);

        if (resource.get("roles") == null) {
            return List.of();
        }

        resourceRoles = (Collection<String>) resource.get("roles");

        return resourceRoles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_".concat(role)))
                .toList();
    }

    /**
     * Devuelve el valor del claim "sub" del JWT, que se
     * utiliza como identificador del usuario autenticado.
     *
     * @return el identificador del usuario autenticado
     */
    @Override
    public String getId() {
        return (String) jwtToken.getClaims().get("sub");
    }

}
