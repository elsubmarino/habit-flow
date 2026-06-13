package io.streak.habitflow.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name="activityLogExecutor")
    public Executor activityLogExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); //기본으로 유지할 쓰레드 개수
        executor.setMaxPoolSize(20); //트래픽 몰릴 때 최대 뿜어낼 쓰레드 개수
        executor.setQueueCapacity(200); //쓰레드가 다 차면 대기할 큐크기
        executor.setThreadNamePrefix("AsyncLog-");
        executor.initialize();
        return executor;
    }
}
