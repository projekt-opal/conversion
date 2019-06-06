package de.upb.cs.dice.opal.conversion.config;
//
//import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
//import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
//import org.springframework.context.annotation.Bean;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//import org.springframework.jms.annotation.EnableJms;
//import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
//import org.springframework.jms.config.JmsListenerContainerFactory;
//import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
//import org.springframework.jms.support.converter.MessageConverter;
//import org.springframework.jms.support.converter.MessageType;
//
//import javax.jms.ConnectionFactory;
//

import org.springframework.amqp.core.Queue;

@Configuration
//@EnableAutoConfiguration
//@EnableJms
public class MessageQueueConfiguration {

    @Bean
    public Queue conversionQueue() {
        return new Queue("conversionQueue");
    }

    @Bean
    public Queue civetQueue() {
        return new Queue("civetQueue");
    }

    @Bean
    public Queue writerQueueTripleStore() {
        return new Queue("writerQueueTripleStore");
    }

    @Bean
    public Queue writerQueueCKAN() {
        return new Queue("writerQueueCKAN");
    }

    @Bean
    public FanoutExchange fanout() {
        return new FanoutExchange("writer.fanout");
    }

    @Bean
    public Binding binding1(FanoutExchange fanout, Queue writerQueueTripleStore) {
        return BindingBuilder.bind(writerQueueTripleStore).to(fanout);
    }

    @Bean
    public Binding binding2(FanoutExchange fanout, Queue writerQueueCKAN) {
        return BindingBuilder.bind(writerQueueCKAN).to(fanout);
    }

}
