package com.fullcount.domain;

public enum TransferStatus {
    REQUESTED,          // 양도 요청됨
    PAYMENT_COMPLETED,  // 에스크로 결제 완료
    TICKET_SENT,        // 티켓 전달 완료 (양도자 확인)
    COMPLETED,          // 인수 확정 및 정산 완료
    CANCELLED           // 거래 취소
}
