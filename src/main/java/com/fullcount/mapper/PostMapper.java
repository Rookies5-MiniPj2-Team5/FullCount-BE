package com.fullcount.mapper;

import com.fullcount.domain.Member;
import com.fullcount.domain.Post;
import com.fullcount.domain.Team;
import com.fullcount.dto.PostDto;

public class PostMapper {

    // Entity -> Response DTO 변환
    public static PostDto.PostResponse toResponse(Post post) {
        if (post == null) return null;

        return PostDto.PostResponse.builder()
                .id(post.getId())
                .authorNickname(post.getAuthor().getNickname())
                .teamName(post.getTeam() != null ? post.getTeam().getName() : null)
                .boardType(post.getBoardType().name())
                .title(post.getTitle())
                .content(post.getContent())
                .matchDate(post.getMatchDate())
                .homeTeamName(post.getHomeTeam() != null ? post.getHomeTeam().getName() : null)
                .homeTeamId(post.getHomeTeam() != null ? post.getHomeTeam().getId().toString() : null)
                .awayTeamName(post.getAwayTeam() != null ? post.getAwayTeam().getName() : null)
                .awayTeamId(post.getAwayTeam() != null ? post.getAwayTeam().getId().toString() : null)
                .ticketPrice(post.getTicketPrice())
                .maxParticipants(post.getMaxParticipants())
                .status(post.getStatus().name())
                .viewCount(post.getViewCount())
                .createdAt(post.getCreatedAt())
                .build();
    }

    // Request DTO -> Entity 변환
    public static Post toEntity(PostDto.CreatePostRequest req, Member author, Team homeTeam, Team awayTeam) {
        return Post.builder()
                .author(author)
                .team(author.getTeam())
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .boardType(req.getBoardType())
                .title(req.getTitle())
                .content(req.getContent())
                .matchDate(req.getMatchDate())
                .ticketPrice(req.getTicketPrice())
                .maxParticipants(req.getMaxParticipants())
                .build();
    }
}