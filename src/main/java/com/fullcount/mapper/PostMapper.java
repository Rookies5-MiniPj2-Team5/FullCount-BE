package com.fullcount.mapper;

import com.fullcount.domain.Member;
import com.fullcount.domain.Post;
import com.fullcount.domain.BoardType;
import com.fullcount.dto.PostDto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PostMapper {

    // === Entity -> Response DTO 변환 ===
    public static PostDto.PostResponse toResponse(Post post) {
        if (post == null) return null;

        // Switch Expression을 사용하여 각 타입별 빌더 생성
        return switch (post.getBoardType()) {
            case MATE -> toMateResponse(post);
            case CREW -> toCrewResponse(post);
            case TRANSFER -> toTransferResponse(post);
            default -> null; // 혹은 예외 처리
        };
    }

    private static PostDto.MateResponse toMateResponse(Post post) {
        var builder = PostDto.MateResponse.builder();
        applyCommonResponse(builder, post);
        return builder
                .matchDate(post.getMatchDate())
                .stadium(post.getStadium() != null ? post.getStadium() : getHomeStadium(post))
                .homeTeamName(getName(post.getHomeTeam()))
                .awayTeamName(getName(post.getAwayTeam()))
                .authorTeam(getName(post.getTeam() != null
                        ? post.getTeam()
                        : post.getSupportTeam()))
                .profileImage(post.getAuthor() != null ? post.getAuthor().getProfileImageUrl() : null)
                .viewCount(post.getViewCount())
                .build();
    }

    private static PostDto.CrewResponse toCrewResponse(Post post) {
        var builder = PostDto.CrewResponse.builder();
        applyCommonResponse(builder, post);
        return builder
                .supportTeamName(getName(post.getSupportTeam()))
                .maxParticipants(post.getMaxParticipants())
                .currentParticipants(post.getParticipants() != null ? post.getParticipants().size() : 0)
                .stadium(post.getStadium())
                .matchDate(post.getMatchDate())
                .matchTime(post.getMatchTime())
                .seatArea(post.getSeatArea())
                .tags(parseTags(post.getTags()))
                .build();
    }

    private static PostDto.TransferResponse toTransferResponse(Post post) {
        var builder = PostDto.TransferResponse.builder();
        applyCommonResponse(builder, post);
        return builder
                .matchDate(post.getMatchDate())
                .homeTeamName(getName(post.getHomeTeam()))
                .awayTeamName(getName(post.getAwayTeam()))
                .seatArea(post.getSeatArea())
                .ticketPrice(post.getTicketPrice())
                .build();
    }

    // === Request DTO -> Entity 변환 ===
    public static Post toEntity(PostDto.CreatePostRequest req, Member author) {
        if (req == null) return null;

        Post.PostBuilder builder = Post.builder()
                .author(author)
                .team(author.getTeam())
                .boardType(req.getBoardType())
                .title(req.getTitle())
                .content(req.getContent());

        // Java 17+ Pattern Matching for instanceof 활용
        if (req instanceof PostDto.CreateMateRequest mateReq) {
            builder.matchDate(mateReq.getMatchDate())
                    .matchTime(mateReq.getMatchTime())
                    .stadium(mateReq.getStadium())
                    .maxParticipants(mateReq.getMaxParticipants());
        } else if (req instanceof PostDto.CreateCrewRequest crewReq) {
            builder.matchDate(crewReq.getMatchDate())
                    .matchTime(crewReq.getMatchTime())
                    .stadium(crewReq.getStadium())
                    .maxParticipants(crewReq.getMaxParticipants())
                    .isPublic(crewReq.getIsPublic())
                    .tags(joinTags(crewReq.getTags()))
                    .seatArea(crewReq.getSeatArea());
        }
        else if (req instanceof PostDto.CreateTransferRequest transferReq) {
            builder.matchDate(transferReq.getMatchDate())
                    .seatArea(transferReq.getSeatArea())
                    .ticketPrice(transferReq.getTicketPrice());
        }

        return builder.build();
    }

    // --- 내부 헬퍼 메서드 (중복 제거용) ---

    /** 공통 응답 필드 설정을 위한 메서드 */
    private static void applyCommonResponse(PostDto.PostResponse.PostResponseBuilder<?, ?> builder, Post post) {
        builder.id(post.getId())
                .authorNickname(post.getAuthor().getNickname())
                .title(post.getTitle())
                .content(post.getContent())
                .boardType(post.getBoardType())
                .status(post.getStatus() != null ? post.getStatus().name() : null)
                .viewCount(post.getViewCount())
                .createdAt(post.getCreatedAt());
    }

    private static String getName(com.fullcount.domain.Team team) {
        return team != null ? team.getName() : null;
    }

    private static String getHomeStadium(Post post) {
        return post.getHomeTeam() != null ? post.getHomeTeam().getHomeStadium() : null;
    }

    private static List<String> parseTags(String tags) {
        return (tags != null && !tags.isEmpty())
                ? Arrays.asList(tags.split(","))
                : Collections.emptyList();
    }

    private static String joinTags(List<String> tags) {
        return (tags != null && !tags.isEmpty())
                ? String.join(",", tags)
                : null;
    }
}
