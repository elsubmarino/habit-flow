package io.streak.habitflow.global.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    String key(); // 락의 식별자 키
    long waitTime() default 5L; //락 획득을 위해 기다릴 시간 (초)
    long leaseTime() default 3L; //락을 획득 후 자동으로 해제될 시간 (초)
}
