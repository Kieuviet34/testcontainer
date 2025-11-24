package learning.executor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConsumerConfig {

    @Bean
    public ConnectionFactory connectionFactory() {
        String host = System.getenv().getOrDefault("SPRING_RABBITMQ_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("SPRING_RABBITMQ_PORT", "5672"));
        String user = System.getenv().getOrDefault("SPRING_RABBITMQ_USERNAME", "guest");
        String pass = System.getenv().getOrDefault("SPRING_RABBITMQ_PASSWORD", "guest");

        CachingConnectionFactory cf = new CachingConnectionFactory(host, port);
        cf.setUsername(user);
        cf.setPassword(pass);
        cf.setChannelCacheSize(25);
        return cf;
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper mapper) {
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(4);
        factory.setPrefetchCount(1);
        return factory;
    }
}
