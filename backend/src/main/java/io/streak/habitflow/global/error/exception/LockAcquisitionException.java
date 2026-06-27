package io.streak.habitflow.global.error.exception;

/**
 * 분산 락 획득 실패 시 사용 (HTTP 503).
 */
public class LockAcquisitionException extends RuntimeException {
    public LockAcquisitionException(String message) {
        super(message);
    }
}
