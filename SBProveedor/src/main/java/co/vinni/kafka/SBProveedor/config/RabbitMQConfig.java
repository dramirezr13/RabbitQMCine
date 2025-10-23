package co.vinni.kafka.SBProveedor.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Queue principal para reservas
    @Bean
    public Queue reservasQueue() {
        return new Queue("reservas.queue", true);
    }

    // 1. DIRECT EXCHANGE (Para consultas específicas)
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange("direct.exchange");
    }

    // 2. FANOUT EXCHANGE (Para broadcast)
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange("fanout.exchange");
    }

    // 3. TOPIC EXCHANGE (Para patrones)
    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange("topic.exchange");
    }

    // Bindings para Direct Exchange
    @Bean
    public Binding directBinding(Queue reservasQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(reservasQueue)
                .to(directExchange)
                .with("reserva.direct");
    }

    // Bindings para Fanout Exchange
    @Bean
    public Binding fanoutBinding(Queue reservasQueue, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(reservasQueue)
                .to(fanoutExchange);
    }

    // Bindings para Topic Exchange
    @Bean
    public Binding topicBinding(Queue reservasQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(reservasQueue)
                .to(topicExchange)
                .with("reservas.#");
    }
}