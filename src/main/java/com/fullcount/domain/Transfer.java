package com.fullcount.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "transfer")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, unique = true)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Member seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private Member buyer;

    @Column(nullable = false)
    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransferStatus status = TransferStatus.REQUESTED;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // ────── 비즈니스 메서드 ──────

    /** 에스크로 결제 완료 처리 */
    public void payEscrow(Member buyer) {
        if (this.status != TransferStatus.REQUESTED) {
            throw new IllegalStateException("결제는 REQUESTED 상태에서만 가능합니다.");
        }
        this.buyer = buyer;
        this.status = TransferStatus.PAYMENT_COMPLETED;
    }

    /** 양도자가 티켓 전달 완료 처리 */
    public void markTicketSent() {
        if (this.status != TransferStatus.PAYMENT_COMPLETED) {
            throw new IllegalStateException("결제 완료 후에만 티켓 전달 처리가 가능합니다.");
        }
        this.status = TransferStatus.TICKET_SENT;
    }

    /** 양수자 인수 확정 → 정산 완료 */
    public void confirmTransfer() {
        if (this.status != TransferStatus.TICKET_SENT && this.status != TransferStatus.PAYMENT_COMPLETED) {
            throw new IllegalStateException("인수 확정은 결제 완료 이후 단계에서만 가능합니다.");
        }
        this.status = TransferStatus.COMPLETED;
    }

    /** 거래 취소 */
    public void cancelTransfer() {
        if (this.status == TransferStatus.COMPLETED) {
            throw new IllegalStateException("이미 완료된 거래는 취소할 수 없습니다.");
        }
        this.status = TransferStatus.CANCELLED;
    }
}
