package io.streak.habitflow.global.error.exception;

import io.streak.habitflow.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String clientMessage;
    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), null);
    }
    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.clientMessage = message != null ? message : errorCode.getDefaultMessage();
    }
}