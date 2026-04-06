package com.fullcount.controller;

import com.fullcount.dto.PostDto;
import com.fullcount.dto.TransferDto;
import com.fullcount.dto.common.PagedResponse;
import com.fullcount.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Transfer", description = "티켓 양도 에스크로 API")
@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferService transferService;

    @Operation(summary = "양도 요청 + 1:1 채팅방 자동 생성")
    @PostMapping("/{postId}/request")
    public ResponseEntity<TransferDto.TransferRequestResponse> requestTransfer(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transferService.requestTransfer(postId, memberId));
    }

    @Operation(summary = "에스크로 결제 완료 처리")
    @PostMapping("/{transferId}/pay")
    public ResponseEntity<TransferDto.TransferStatusResponse> payEscrow(
            @PathVariable Long transferId,
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(transferService.payEscrow(transferId, memberId));
    }

    @Operation(summary = "티켓 전달 완료 확인 (양도자)")
    @PostMapping("/{transferId}/ticket-sent")
    public ResponseEntity<TransferDto.TransferStatusResponse> markTicketSent(
            @PathVariable Long transferId,
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(transferService.markTicketSent(transferId, memberId));
    }

    @Operation(summary = "인수 확정 + 정산 완료 (양수자)")
    @PostMapping("/{transferId}/confirm")
    public ResponseEntity<TransferDto.TransferStatusResponse> confirmTransfer(
            @PathVariable Long transferId,
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(transferService.confirmTransfer(transferId, memberId));
    }

    @Operation(summary = "거래 취소")
    @PostMapping("/{transferId}/cancel")
    public ResponseEntity<TransferDto.TransferStatusResponse> cancelTransfer(
            @PathVariable Long transferId,
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(transferService.cancelTransfer(transferId, memberId));
    }

    @Operation(summary = "내가 신청한 양도 내역 조회")
    @GetMapping("/me")
    public ResponseEntity<PagedResponse<PostDto.PostResponse>> getMyTransfers(
            @AuthenticationPrincipal Long memberId,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(transferService.getMyTransfers(memberId, pageable));
    }
}