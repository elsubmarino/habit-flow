package io.streak.habitflow.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

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

        // 큐가 가득 차면 버리기(Abort) 대신, 호출한 스레드가 직접 실행
        // → RejectedExecutionException으로 로그/알림이 유실되지 않음
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 앱 종료 시 진행 중인 비동기 작업을 끝까지 기다림
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }

    @Bean(name="mailExecutor")
    public Executor mailExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); //기본으로 유지할 쓰레드 개수
        executor.setMaxPoolSize(15); //트래픽 몰릴 때 최대 뿜어낼 쓰레드 개수
        executor.setQueueCapacity(100); //쓰레드가 다 차면 대기할 큐크기
        executor.setThreadNamePrefix("MailAsync-");

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}
