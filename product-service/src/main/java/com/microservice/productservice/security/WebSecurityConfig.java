package com.microservice.productservice.security;

import com.microservice.productservice.security.jwt.AuthEntryPointJwt;
import com.microservice.productservice.security.jwt.AuthTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    private final AuthEntryPointJwt unauthorizedHandler;
    private final AuthTokenFilter authTokenFilter;

    public WebSecurityConfig(AuthEntryPointJwt unauthorizedHandler, AuthTokenFilter authTokenFilter) {
        this.unauthorizedHandler = unauthorizedHandler;
        this.authTokenFilter = authTokenFilter;
    }

    @Bean
    public WebClient.Builder securityWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth ->
                auth.requestMatchers("/api/products/all", "/api/products/all-products", 
                                  "/api/products/shop-products", "/api/products/sort-products",
                                  "/api/products/{id:[\\d]+}", "/api/products/search",
                                  "/api/products/category/**", "/api/products/brand/**",
                                  "/api/products/price-range").permitAll()
                    .requestMatchers("/api/products/add", "/api/products/{id}", 
                                  "/api/products/low-stock", "/api/products/{id}/reorder-level",
                                  "/api/products/batch-inventory-update",
                                  "/api/products/import-excel").hasRole("ADMIN")
                    .anyRequest().authenticated()
            );

        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}