package com.example.personalinfoservice.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Interim security configuration.
 *
 * <p>{@code spring-boot-starter-security-oauth2-client} is on the classpath
 * for a future OAuth2 Client login flow, but Spring Security's default
 * auto-configuration locks every endpoint behind HTTP Basic + a generated
 * password as soon as that starter is present — even though no
 * authentication mechanism has actually been decided for this project yet
 * (see docs/api/contracts.md, "Authentication & authorization — NOT YET
 * DECIDED"). Without this bean, {@code GET /api/v1/technologies} (and every
 * future endpoint) would 401 by default, which contradicts the documented
 * "no auth for now" contract.
 *
 * <p>This permits all requests and disables CSRF (no session-based state
 * changes exist yet). It intentionally does not implement or assume any
 * particular auth mechanism — replace it with real authorization rules once
 * that decision is made (see ADR log / contracts.md) rather than layering
 * rules on top of it.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }
}
