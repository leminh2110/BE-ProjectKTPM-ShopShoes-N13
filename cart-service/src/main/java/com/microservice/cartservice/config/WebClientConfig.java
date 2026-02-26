package com.microservice.cartservice.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient.Builder webClientBuilder() {
        // Configure memory limit for responses (5MB)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
                .build();
        
        // Configure timeout settings
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // Connection timeout 5s
                .responseTimeout(Duration.ofSeconds(10)) // Response timeout 10s
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));
        
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .filter(logRequest()) // Add request logging
                .filter(logResponse()) // Add response logging
                .filter(retryFilter()); // Add retry filter
    }
    
    // Filter to perform retry
    private ExchangeFilterFunction retryFilter() {
        return (request, next) -> next.exchange(request)
                .retryWhen(Retry.backoff(5, Duration.ofMillis(500)) // Retry 5 times, delay 0.5s
                        .filter(throwable -> {
                            // Retry only for network or server error
                            if (throwable instanceof WebClientResponseException) {
                                WebClientResponseException ex = (WebClientResponseException) throwable;
                                return ex.getStatusCode().is5xxServerError() || 
                                       ex.getStatusCode().value() == 503;
                            }
                            return true; // Retry for all other errors
                        })
                        .doBeforeRetry(retrySignal -> 
                            System.out.println("Retry attempt " + retrySignal.totalRetries() + 
                                             " for " + request.url())));
    }
    
    // Log request filter
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (clientRequest.url().toString().contains("/api/")) {
                System.out.println("Request: " + clientRequest.method() + " " + clientRequest.url());
            }
            return Mono.just(clientRequest);
        });
    }
    
    // Log response filter
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().isError()) {
                System.out.println("Response error: " + clientResponse.statusCode());
            }
            return Mono.just(clientResponse);
        });
    }
} 