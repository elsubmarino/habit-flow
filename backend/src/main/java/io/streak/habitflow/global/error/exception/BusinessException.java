package io.streak.habitflow.global.error.exception;

import io.streak.habitflow.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String clientMessage;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.clientMessage = errorCode.getDefaultMessage();
    }

    public BusinessException(ErrorCode errorCode, String clientMessage) {
        super(clientMessage);
        this.errorCode = errorCode;
        this.clientMessage = clientMessage;
    }

    public BusinessException(
            ErrorCode errorCode,
            String internalMessage,
            Throwable cause
    ) {
        super(internalMessage, cause);
        this.errorCode = errorCode;
        this.clientMessage = errorCode.getDefaultMessage();
    }
}