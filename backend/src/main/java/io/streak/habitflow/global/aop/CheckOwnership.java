package io.streak.habitflow.global.aop;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(CheckOwnerships.class)
public @interface CheckOwnership {
    String type();
}