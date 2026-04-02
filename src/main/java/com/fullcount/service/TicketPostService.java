package com.fullcount.service;

import com.fullcount.domain.Member;
import com.fullcount.domain.TicketPost;
import com.fullcount.domain.TicketPostStatus;
import com.fullcount.dto.TicketPostDto;
import com.fullcount.dto.common.PagedResponse;
import com.fullcount.exception.BusinessException;
import com.fullcount.exception.ErrorCode;
import com.fullcount.repository.MemberRepository;
import com.fullcount.repository.TicketPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketPostService {

    private final TicketPostRepository ticketPostRepository;
    private final MemberRepository memberRepository;

    /** 티켓 목록 조회 (필터 + 페이징) */
    public PagedResponse<TicketPostDto.Response> getTickets(
            LocalDate date, String stadium, String homeTeam, String team, java.util.List<TicketPostStatus> statuses, Pageable pageable) {

        if (statuses == null || statuses.isEmpty()) {
            statuses = java.util.Arrays.asList(TicketPostStatus.SELLING, TicketPostStatus.RESERVED);
        }
        
        Page<TicketPost> page = ticketPostRepository.searchTickets(date, stadium, homeTeam, team, statuses, pageable);
        return PagedResponse.of(page.map(TicketPostDto.Response::from));
    }

    /** 티켓 전체 목록 조회 (단건) */
    public TicketPostDto.Response getTicket(Long ticketPostId) {
        TicketPost ticketPost = findTicketPostOrThrow(ticketPostId);
        return TicketPostDto.Response.from(ticketPost);
    }

    @Transactional
    public TicketPostDto.Response createTicket(Long memberId, TicketPostDto.CreateRequest req) {
        Member author = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        TicketPost ticketPost = TicketPost.builder()
                .title(req.getTitle())
                .content(req.getContent())
                .homeTeam(req.getHomeTeam())
                .awayTeam(req.getAwayTeam())
                .matchDate(req.getMatchDate())
                .matchTime(req.getMatchTime())
                .stadium(req.getStadium())
                .seatArea(req.getSeatArea())
                .seatBlock(req.getSeatBlock())
                .seatRow(req.getSeatRow())
                .price(req.getPrice())
                .author(author)
                .build();

        return TicketPostDto.Response.from(ticketPostRepository.save(ticketPost));
    }

    /** 티켓 양도글 작성 (TicketTransferRequestDTO 기반) */
    @Transactional
    public TicketPostDto.Response createTicketFromDto(Long memberId, TicketPostDto.TicketTransferRequestDTO req) {
        Member author = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 제목 자동 생성: [홈팀 vs 어웨이팀] 경기장 좌석 구역
        String generatedTitle = String.format("[%s vs %s] %s %s", 
                req.getHomeTeam(), req.getAwayTeam(), req.getStadium(), req.getSeatArea());

        TicketPost ticketPost = TicketPost.builder()
                .title(generatedTitle)
                .content(req.getDescription() != null ? req.getDescription() : "")
                .homeTeam(req.getHomeTeam())
                .awayTeam(req.getAwayTeam())
                .matchDate(req.getMatchDate())
                .matchTime(req.getMatchTime())
                .stadium(req.getStadium())
                .seatArea(req.getSeatArea())
                .seatBlock(req.getSeatBlock())
                .seatRow(req.getSeatRow())
                .price(req.getPrice())
                .author(author)
                .build();

        return TicketPostDto.Response.from(ticketPostRepository.save(ticketPost));
    }

    /** 티켓 상태 변경 (작성자 본인만 가능) */
    @Transactional
    public TicketPostDto.Response updateStatus(Long ticketPostId, Long memberId, TicketPostStatus newStatus) {
        TicketPost ticketPost = findTicketPostOrThrow(ticketPostId);

        if (!ticketPost.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        ticketPost.updateStatus(newStatus);
        return TicketPostDto.Response.from(ticketPost);
    }

    /** 게시글 삭제 (작성자 본인만 가능) */
    @Transactional
    public void deleteTicket(Long ticketPostId, Long memberId) {
        TicketPost ticketPost = findTicketPostOrThrow(ticketPostId);

        if (!ticketPost.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        ticketPostRepository.delete(ticketPost);
    }

    // ────── private helper ──────

    private TicketPost findTicketPostOrThrow(Long ticketPostId) {
        return ticketPostRepository.findById(ticketPostId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND));
    }
}
