package com.microservice.productservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${product.queue.order}")
    private String orderQueue;
    
    @Value("${product.queue.inventory}")
    private String inventoryQueue;

    // Define Queues
    @Bean
    public Queue orderQueue() {
        return new Queue(orderQueue, true);
    }

    @Bean
    public Queue inventoryQueue() {
        return new Queue(inventoryQueue, true);
    }

    // Define Exchange
    @Bean
    public DirectExchange productExchange() {
        return new DirectExchange("product.exchange");
    }

    // Bind queues to exchange
    @Bean
    public Binding orderBinding(Queue orderQueue, DirectExchange productExchange) {
        return BindingBuilder.bind(orderQueue).to(productExchange).with("product.order");
    }

    @Bean
    public Binding inventoryBinding(Queue inventoryQueue, DirectExchange productExchange) {
        return BindingBuilder.bind(inventoryQueue).to(productExchange).with("product.inventory");
    }

    // Message converter for JSON serialization/deserialization
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // Configure RabbitTemplate
    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
} 