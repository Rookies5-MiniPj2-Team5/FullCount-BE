package com.fullcount.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 인증/인가
    INVALID_CREDENTIALS("AUTH_001", "이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("AUTH_002", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    INVALID_TOKEN("AUTH_003", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("AUTH_004", "만료된 토큰입니다.", HttpStatus.UNAUTHORIZED),

    // 회원
    MEMBER_NOT_FOUND("MEM_001", "존재하지 않는 회원입니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_EMAIL("MEM_002", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
    DUPLICATE_NICKNAME("MEM_003", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),
    TEAM_CHANGE_LIMIT("MEM_004", "이번 시즌 팀 변경 횟수를 초과했습니다.", HttpStatus.BAD_REQUEST),

    // 게시글
    POST_NOT_FOUND("POST_001", "존재하지 않는 게시글입니다.", HttpStatus.NOT_FOUND),
    POST_NOT_EDITABLE("POST_002", "예약 중이거나 마감된 게시글은 수정/삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    TEAM_BOARD_ACCESS_DENIED("POST_003", "해당 팀 전용 게시판에 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    TICKET_PRICE_EXCEEDED("POST_004", "양도 가격은 정가를 초과할 수 없습니다.", HttpStatus.BAD_REQUEST),

    // 양도 거래
    TRANSFER_NOT_FOUND("TRF_001", "존재하지 않는 거래입니다.", HttpStatus.NOT_FOUND),
    TRANSFER_INVALID_STATUS("TRF_002", "현재 거래 상태에서 해당 작업을 수행할 수 없습니다.", HttpStatus.BAD_REQUEST),
    TRANSFER_ALREADY_EXISTS("TRF_003", "이미 거래가 진행 중인 게시글입니다.", HttpStatus.CONFLICT),
    TRANSFER_SELF_NOT_ALLOWED("TRF_004", "자신의 게시글에는 양도 요청할 수 없습니다.", HttpStatus.BAD_REQUEST),

    // 팀
    TEAM_NOT_FOUND("TEAM_001", "존재하지 않는 팀입니다.", HttpStatus.NOT_FOUND),

    // 채팅
    CHAT_ROOM_NOT_FOUND("CHAT_001", "존재하지 않는 채팅방입니다.", HttpStatus.NOT_FOUND),

    // 서버
    INTERNAL_SERVER_ERROR("SERVER_001", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR("SERVER_002", "입력값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}
