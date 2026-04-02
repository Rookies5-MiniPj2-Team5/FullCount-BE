package com.fullcount.repository;

import com.fullcount.domain.QTicketPost;
import com.fullcount.domain.TicketPost;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TicketPostRepositoryImpl implements TicketPostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<TicketPost> searchTickets(LocalDate date, String stadium, String homeTeam, String team, java.util.List<com.fullcount.domain.TicketPostStatus> statuses, Pageable pageable) {
        QTicketPost ticketPost = QTicketPost.ticketPost;

        BooleanBuilder where = new BooleanBuilder();

        // 날짜 필터
        if (date != null) {
            where.and(ticketPost.matchDate.eq(date));
        }

        // 구장 필터 (부분 일치)
        if (stadium != null && !stadium.isBlank()) {
            where.and(ticketPost.stadium.containsIgnoreCase(stadium));
        }

        // 홈팀 필터
        if (homeTeam != null && !homeTeam.isBlank()) {
            where.and(ticketPost.homeTeam.containsIgnoreCase(homeTeam));
        }

        // 팀 필터 - homeTeam OR awayTeam
        if (team != null && !team.isBlank()) {
            where.and(
                    ticketPost.homeTeam.containsIgnoreCase(team)
                            .or(ticketPost.awayTeam.containsIgnoreCase(team))
            );
        }

        // 상태 필터 (IN 조건)
        if (statuses != null && !statuses.isEmpty()) {
            where.and(ticketPost.status.in(statuses));
        }

        List<TicketPost> content = queryFactory
                .selectFrom(ticketPost)
                .join(ticketPost.author).fetchJoin()  // N+1 방지
                .where(where)
                .orderBy(ticketPost.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .select(ticketPost.count())
                .from(ticketPost)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }
}
