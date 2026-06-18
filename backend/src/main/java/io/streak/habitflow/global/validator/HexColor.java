package io.streak.habitflow.global.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = HexColorValidator.class)
@Target({ElementType.FIELD,ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface HexColor {
    String message() default "올바른 Hex Code 색상 규격이 아닙니다.";
    Class<?>[] groups() default{};
    Class<? extends Payload>[] payload() default {};
}
