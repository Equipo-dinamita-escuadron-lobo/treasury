package com.treasury.infrastructure.adapters.output.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Autowired
    private JwtAuthConverter jwtAuthConverter;

    /**
     * Este método configura la cadena de filtros de seguridad. Deshabilita la
     * protección CSRF,
     * y para los puntos de conexión /swagger-ui/** y /v3/api-docs/**, permite
     * acceso anónimo.
     * Para todos los demás puntos de conexión, requiere autenticación.
     *
     * También configura el convertidor de autenticación JWT y especifica que la
     * aplicación
     * debe utilizar JWTs para la autenticación.
     *
     * Finalmente, establece la política de creación de sesión en STATELESS.
     *
     * @param httpSecurity El objeto HttpSecurity a configurar
     * @return La cadena de filtros de seguridad a utilizar
     * @throws Exception Si hay un error al configurar la cadena de filtros de
     *                   seguridad
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(http -> http
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**","/actuator/**").permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth -> {
                    oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter));
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

}
