package io.streak.habitflow.global.aop;

import io.streak.habitflow.global.aop.transactionTemplate.AopForTransaction;
import io.streak.habitflow.global.error.ErrorCode;
import io.streak.habitflow.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
@Order(1) //Transactional 보다 머저 락을 거머쥐어야 함
@RequiredArgsConstructor
public class DistributedLockAspect {
    private final RedissonClient rediSsonClient;
    private final AopForTransaction aopForTransaction; //트랜잭션 분리용 헬퍼 컴포넌트

    @Around("@annotation(distributedLock))")
    public Object lock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        //SpEL 인터프리터 등을 활용해 메서드 인자에서 key 값을 파싱함
        String lockKey = "LOCK:"+distributedLock.key();
        RLock rLock = rediSsonClient.getLock(lockKey);

        try{
            //레디스 분산 락 획득 시도 (waitTime 동안 대기, leaseTime 지나면 자동 소멸)
            boolean available = rLock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), TimeUnit.SECONDS);
            if(!available){
                throw new BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED, "락 획득 실패 - 트래픽 초과");
            }

            //락 획득 성공 시, 트랜잭션 새로 열어서 실제 비지니스 로직 실행 및 커밋까지 완료
            return aopForTransaction.proceed(joinPoint);
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED, "락 대기 중 인터럽트가 발생했습니다.");
        }finally{
            //비지니스 로직과 DB 커밋이 완전히 끝난 후 안전하게 레디스 락 해제
            if(rLock.isHeldByCurrentThread()){
                rLock.unlock();
            }
        }
    }
}
