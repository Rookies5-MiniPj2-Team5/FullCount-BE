package com.fullcount.controller;


import com.fullcount.dto.TicketPostDto;
import com.fullcount.dto.common.PagedResponse;
import com.fullcount.service.TicketPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "TicketPost", description = "KBO 티켓 양도 게시판 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TicketPostController {

    private final TicketPostService ticketPostService;

    @Operation(summary = "티켓 양도 목록 조회 (날짜·구장·홈팀·상태 필터 + 페이징)")
    @GetMapping({"/tickets", "/ticket-transfers"})
    public ResponseEntity<PagedResponse<TicketPostDto.Response>> getTickets(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate matchDate,
            @RequestParam(required = false) String stadium,
            @RequestParam(required = false) String homeTeam,
            @RequestParam(required = false) String team, // 기존 하위 호환
            @RequestParam(required = false) java.util.List<com.fullcount.domain.TicketPostStatus> status,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(ticketPostService.getTickets(matchDate, stadium, homeTeam, team, status, pageable));
    }

    @Operation(summary = "티켓 양도 단건 조회")
    @GetMapping({"/tickets/{id}", "/ticket-transfers/{id}"})
    public ResponseEntity<TicketPostDto.Response> getTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ticketPostService.getTicket(id));
    }

    @Operation(summary = "티켓 양도글 작성 (기존)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/tickets")
    public ResponseEntity<TicketPostDto.Response> createTicket(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody TicketPostDto.CreateRequest req) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketPostService.createTicket(memberId, req));
    }

    @Operation(summary = "티켓 양도 글쓰기 (사용자 요청 스펙)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/ticket-transfers")
    public ResponseEntity<TicketPostDto.Response> createTicketTransfer(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody TicketPostDto.TicketTransferRequestDTO req) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketPostService.createTicketFromDto(memberId, req));
    }

    @Operation(summary = "티켓 상태 변경 (SELLING → RESERVED → SOLD, 작성자 본인만)")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketPostDto.Response> updateStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody TicketPostDto.UpdateStatusRequest req) {

        return ResponseEntity.ok(ticketPostService.updateStatus(id, memberId, req.getStatus()));
    }

    @Operation(summary = "티켓 양도글 삭제 (작성자 본인만)")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId) {

        ticketPostService.deleteTicket(id, memberId);
        return ResponseEntity.noContent().build();
    }
}
