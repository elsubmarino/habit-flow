package io.streak.habitflow.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", "요청이 너무 많습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다"),
    // 400
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "요청 값이 올바르지 않습니다."),
    INVALID_LABEL(HttpStatus.BAD_REQUEST, "INVALID_LABEL", "존재하지 않는 라벨이 있습니다."),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "VERIFICATION_CODE_EXPIRED",
            "인증번호가 만료되었거나 존재하지 않습니다."),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "VERIFICATION_CODE_MISMATCH",
            "인증번호가 일치하지 않습니다."),
    // 403
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "접근 권한이 없습니다."),
    UNVERIFIED_EMAIL(HttpStatus.FORBIDDEN, "UNVERIFIED_EMAIL",
            "이메일 본인 인증이 완료되지 않았습니다."),
    // 409
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 가입된 이메일입니다."),
    DUPLICATE_PROJECT_MEMBER(HttpStatus.CONFLICT, "DUPLICATE_PROJECT_MEMBER", null), // 동적 메시지
    TASK_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "TASK_LIMIT_EXCEEDED",
            "해당 프로젝트에 테스크를 500개까지 보유할 수 있습니다."),
    PROJECT_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "PROJECT_LIMIT_EXCEEDED",
            "보유하고 있는 프로젝트가 500개 이상이므로 더 이상 생성할 수 없습니다."),
    SUB_TASK_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "SUB_TASK_LIMIT_EXCEEDED",
            "하위 테스크는 최대 4개 까지만 생성할 수 있습니다."),
    // 410
    INVITE_LINK_EXPIRED(HttpStatus.GONE, "INVITE_LINK_EXPIRED",
            "만료되었거나 유효하지 않은 초대 링크입니다."),
    // 429
    MAIL_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "MAIL_RATE_LIMIT",
            "인증번호는 1분에 한 번만 요청할 수 있습니다."),
    // 503
    LOCK_ACQUISITION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "LOCK_ACQUISITION_FAILED",
            "락 획득 실패 - 트래픽 초과"),
    // 500
    FILE_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_STORAGE_FAILED",
            "파일 저장 중 오류가 발생했습니다."),
    SSE_CONNECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SSE_CONNECTION_FAILED",
            "실시간 알림 연결에 실패하였습니다."),
    DATA_CONVERSION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "DATA_CONVERSION_FAILED",
            "데이터 처리 중 오류가 발생했습니다."),
    ;
    private final HttpStatus status;
    private final String code;          // 프론트/i18n용
    private final String defaultMessage; // null이면 throw 시 메시지 사용
}