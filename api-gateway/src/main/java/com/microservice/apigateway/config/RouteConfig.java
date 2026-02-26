package com.microservice.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import reactor.core.publisher.Mono;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Authentication Service Routes - Auth endpoints
                .route("auth-service", r -> r.path("/api/auth/**")
                        .filters(f -> f
                                .addRequestHeader("Authorization", "${header.Authorization}")
                                .circuitBreaker(
                                        config -> config.setName("authServiceCircuitBreaker")
                                                .setFallbackUri("forward:/fallback/auth-service")))
                        .uri("lb://auth-service"))
                
                // Authentication Service Routes - User management endpoints (plural form)
                .route("user-management", r -> r.path("/api/users/**")
                        .filters(f -> f.circuitBreaker(
                                config -> config.setName("userManagementCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/auth-service")))
                        .uri("lb://auth-service"))
                
                // Authentication Service Routes - User management endpoints (singular form)
                .route("user-management-singular", r -> r.path("/api/user/**")
                        .filters(f -> f
                                .rewritePath("/api/user/(?<segment>.*)", "/api/users/${segment}")
                                .circuitBreaker(
                                    config -> config.setName("userManagementCircuitBreaker")
                                            .setFallbackUri("forward:/fallback/auth-service")))
                        .uri("lb://auth-service"))
                
                // Product Service Routes
                .route("product-service", r -> r.path("/api/products/**")
                        .filters(f -> f
                                .addRequestHeader("Authorization", "${header.Authorization}")
                                .circuitBreaker(
                                        config -> config.setName("productServiceCircuitBreaker")
                                                .setFallbackUri("forward:/fallback/product-service")))
                        .uri("lb://product-service"))
                
                // Invoice Service Routes
                .route("invoice-service", r -> r.path("/api/v1/invoices/**")
                        .filters(f -> f
                                .addRequestHeader("Authorization", "${header.Authorization}")
                                .circuitBreaker(
                                        config -> config.setName("invoiceServiceCircuitBreaker")
                                                .setFallbackUri("forward:/fallback/invoice-service")))
                        .uri("lb://invoice-service"))
                
                // Cart Service Routes with Rate Limiter
                .route("cart-service", r -> r.path("/api/carts/**")
                        .filters(f -> f
                                .addRequestHeader("Authorization", "${header.Authorization}")
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver())
                                        .setStatusCode(HttpStatus.TOO_MANY_REQUESTS))
                                .addResponseHeader("X-Rate-Limit-Remaining", "1")
                                .addResponseHeader("X-Rate-Limit-Duration", "1")
                                .addResponseHeader("X-Rate-Limit-Reset", "1")
                                .circuitBreaker(
                                        config -> config.setName("cartServiceCircuitBreaker")
                                                .setFallbackUri("forward:/fallback/cart-service")))
                        .uri("lb://cart-service"))
                
                // Email Service Routes
                .route("emailsender-service", r -> r.path("/api/email/**")
                        .filters(f -> f
                                .addRequestHeader("Authorization", "${header.Authorization}")
                                .circuitBreaker(
                                        config -> config.setName("emailServiceCircuitBreaker")
                                                .setFallbackUri("forward:/fallback/emailsender-service")))
                        .uri("lb://emailsender-service"))
                
                // Payment Service Routes
                .route("payment-service", r -> r.path("/api/v1/payments/**")
                        .filters(f -> f
                                .addRequestHeader("Authorization", "${header.Authorization}")
                                .circuitBreaker(
                                        config -> config.setName("paymentServiceCircuitBreaker")
                                                .setFallbackUri("forward:/fallback/payment-service")))
                        .uri("lb://payment-service"))
                
                .build();
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(1, 1); // replenishRate=1, burstCapacity=1
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String user = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (user == null) {
                return Mono.just("anonymous");
            }
            return Mono.just(user);
        };
    }
} 