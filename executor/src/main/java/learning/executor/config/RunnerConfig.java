package learning.executor.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RunnerConfig {
    public static final String QUEUE = "code.requests";
    @Bean
    public Queue codeQueue(){
        return new Queue(QUEUE, true);
    }
}
