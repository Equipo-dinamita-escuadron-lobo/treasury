package com.treasury.infrastructure.adapters.config;

import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;

@Configuration
public class RabbitConfig {
    public static final String TREASURY_EXCHANGE = "treasury.exchange";
    public static final String TREASURY_TRANSACTION_QUEUE = "treasury.transaction.queue";

    @Bean
    Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    Queue treasuryTransactionQueue() {
        return new Queue(TREASURY_TRANSACTION_QUEUE, true);
    }

    @Bean
    FanoutExchange treasuryExchange() {
        return new FanoutExchange(TREASURY_EXCHANGE, true, false);
    }

    @Bean
    Binding treasuryTransactionQueueBinding() {
        return BindingBuilder.bind(treasuryTransactionQueue()).to(treasuryExchange());
    }

}
