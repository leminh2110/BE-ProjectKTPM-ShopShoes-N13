package com.microservice.productservice.client;

import com.microservice.productservice.dto.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class UserClient {
    private final WebClient webClient;

    @Value("${api.gateway.url}")
    private String apiGatewayUrl;

    public UserClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8081").build();
    }

    public UserDto getUserById(Long userId, String authToken) {
        return webClient.get()
                .uri("/api/users/{id}", userId)
                .header("Authorization", "Bearer " + authToken)
                .retrieve()
                .bodyToMono(UserDto.class)
                .doOnError(error -> log.error("Error fetching user with ID {}: {}", userId, error.getMessage()))
                .block();
    }
}