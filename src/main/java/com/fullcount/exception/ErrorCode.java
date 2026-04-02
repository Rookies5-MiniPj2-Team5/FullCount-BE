package com.fullcount.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 인증/인가
    UNAUTHORIZED("AUTH_001", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("AUTH_002", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    INVALID_CREDENTIALS("AUTH_003", "이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("AUTH_004", "유효하지 않은 인증 토큰입니다.", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("AUTH_005", "만료된 인증 토큰입니다.", HttpStatus.UNAUTHORIZED),
    INACTIVE_MEMBER("AUTH_006", "비활성화된 회원입니다.", HttpStatus.FORBIDDEN),

    // 회원
    MEMBER_NOT_FOUND("MEM_001", "존재하지 않는 회원입니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_EMAIL("MEM_002", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
    DUPLICATE_NICKNAME("MEM_003", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),
    TEAM_CHANGE_LIMIT("MEM_004", "이번 시즌 팀 변경 횟수를 초과했습니다.", HttpStatus.BAD_REQUEST),
    ALREADY_IN_TEAM("MEM_005", "이미 선택한 팀과 동일한 팀으로 변경할 수 없습니다.", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD("MEM_006", "현재 비밀번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST), // ⭐️ 수정된 부분

    // 게시글
    POST_NOT_FOUND("POST_001", "존재하지 않는 게시글입니다.", HttpStatus.NOT_FOUND),
    POST_NOT_EDITABLE("POST_002", "예약 중이거나 마감된 게시글은 수정/삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    TEAM_BOARD_ACCESS_DENIED("POST_003", "해당 팀 전용 게시판에 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    TICKET_PRICE_EXCEEDED("POST_004", "양도 가격은 정가를 초과할 수 없습니다.", HttpStatus.BAD_REQUEST),
    INVALID_BOARD_TYPE("POST_005", "해당 게시판 타입에 유효하지 않은 요청입니다.", HttpStatus.BAD_REQUEST),
    CREW_FULL("POST_006", "모집 인원이 가득 찼습니다.", HttpStatus.BAD_REQUEST),
    ALREADY_PARTICIPATING("POST_007", "이미 참여 중인 크루입니다.", HttpStatus.BAD_REQUEST),

    // 티켓 양도 게시판
    TICKET_NOT_FOUND("TCK_001", "존재하지 않는 티켓 양도 게시글입니다.", HttpStatus.NOT_FOUND),

    // 양도 거래
    TRANSFER_NOT_FOUND("TRF_001", "존재하지 않는 거래입니다.", HttpStatus.NOT_FOUND),
    TRANSFER_INVALID_STATUS("TRF_002", "현재 거래 상태에서 해당 작업을 수행할 수 없습니다.", HttpStatus.BAD_REQUEST),
    TRANSFER_ALREADY_EXISTS("TRF_003", "이미 거래가 진행 중인 게시글입니다.", HttpStatus.CONFLICT),
    TRANSFER_SELF_NOT_ALLOWED("TRF_004", "자신의 게시글에는 양도 요청할 수 없습니다.", HttpStatus.BAD_REQUEST),

    TRANSFER_PAYMENT_NOT_ALLOWED("TRF_005", "결제는 REQUESTED 상태에서만 가능합니다.", HttpStatus.BAD_REQUEST),
    TRANSFER_TICKET_SEND_NOT_ALLOWED("TRF_006", "결제 완료 후에만 티켓 전달 처리가 가능합니다.", HttpStatus.BAD_REQUEST),
    TRANSFER_CONFIRM_NOT_ALLOWED("TRF_007", "인수 확정은 결제 완료 이후 단계에서만 가능합니다.", HttpStatus.BAD_REQUEST),
    TRANSFER_CANCEL_NOT_ALLOWED("TRF_008", "이미 완료된 거래는 취소할 수 없습니다.", HttpStatus.BAD_REQUEST),

    INSUFFICIENT_BALANCE("PAY_001", "잔액이 부족합니다.", HttpStatus.BAD_REQUEST),

    // 팀
    TEAM_NOT_FOUND("TEAM_001", "존재하지 않는 팀입니다.", HttpStatus.NOT_FOUND),

    // 채팅
    CHAT_ROOM_NOT_FOUND("CHAT_001", "존재하지 않는 채팅방입니다.", HttpStatus.NOT_FOUND),
    CHAT_ROOM_ACCESS_DENIED("CHAT_002", "해당 채팅방에 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    CHAT_MESSAGE_EMPTY("CHAT_003", "메시지 내용은 비어 있을 수 없습니다.", HttpStatus.BAD_REQUEST),

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