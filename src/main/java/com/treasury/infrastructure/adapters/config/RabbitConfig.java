package com.treasury.infrastructure.adapters.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.*;

@Configuration
public class RabbitConfig {
    public static final String INVOICE_EXCHANGE="purchase.invoice.exchange";
    public static final String PURCHASE_QUEUE="invoice.treasury.queue";
    public static final String ACCOUNTING_EXCHANGE="treasury.accounting.exchange";
    public static final String RESULT_EXCHANGE="accounting.result.exchange";
    public static final String RESULT_QUEUE="accounting.treasury.result.queue";
    public static final String NOTIFICATION_EXCHANGE="payment.notification.exchange";
    public static final String DLX="treasury.dlx";
    public static final String DLQ="treasury.dlq";

    @Bean Jackson2JsonMessageConverter jsonMessageConverter(){return new Jackson2JsonMessageConverter();}
    @Bean FanoutExchange invoiceExchange(){return new FanoutExchange(INVOICE_EXCHANGE,true,false);}
    @Bean FanoutExchange accountingExchange(){return new FanoutExchange(ACCOUNTING_EXCHANGE,true,false);}
    @Bean FanoutExchange resultExchange(){return new FanoutExchange(RESULT_EXCHANGE,true,false);}
    @Bean FanoutExchange notificationExchange(){return new FanoutExchange(NOTIFICATION_EXCHANGE,true,false);}
    @Bean FanoutExchange treasuryDlx(){return new FanoutExchange(DLX,true,false);}
    @Bean Queue purchaseQueue(){return QueueBuilder.durable(PURCHASE_QUEUE).deadLetterExchange(DLX).build();}
    @Bean Queue resultQueue(){return QueueBuilder.durable(RESULT_QUEUE).deadLetterExchange(DLX).build();}
    @Bean Queue treasuryDlq(){return QueueBuilder.durable(DLQ).build();}
    @Bean Binding purchaseBinding(){return BindingBuilder.bind(purchaseQueue()).to(invoiceExchange());}
    @Bean Binding resultBinding(){return BindingBuilder.bind(resultQueue()).to(resultExchange());}
    @Bean Binding dlqBinding(){return BindingBuilder.bind(treasuryDlq()).to(treasuryDlx());}
    @Bean SimpleRabbitListenerContainerFactory treasuryRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, SimpleRabbitListenerContainerFactoryConfigurer configurer) {
        var factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless().maxAttempts(3)
                .backOffOptions(500, 2.0, 5000)
                .recoverer(new RejectAndDontRequeueRecoverer()).build());
        return factory;
    }
}
