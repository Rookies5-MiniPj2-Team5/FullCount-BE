package com.fullcount.repository;

import com.fullcount.domain.TicketPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface TicketPostRepositoryCustom {

    /**
     * 필터 조건을 적용하여 티켓 양도 게시글 목록을 페이징 조회합니다.
     *
     * @param date     특정 경기 날짜 (null이면 전체)
     * @param stadium  구장 명칭 (null이면 전체)
     * @param homeTeam 홈팀 명칭 (null이면 전체)
     * @param team     검색 팀명 (홈/어웨이 무관, null이면 전체)
     * @param statuses 상태 리스트 (null이거나 비어있으면 전체)
     * @param pageable 페이징/정렬 정보
     * @return 필터링된 게시글 페이지
     */
    Page<TicketPost> searchTickets(LocalDate date, String stadium, String homeTeam, String team, java.util.List<com.fullcount.domain.TicketPostStatus> statuses, Pageable pageable);
}
