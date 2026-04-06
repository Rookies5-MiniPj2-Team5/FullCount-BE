package com.fullcount.service;

import com.fullcount.domain.*;
import com.fullcount.dto.PostDto;
import com.fullcount.dto.common.PagedResponse;
import com.fullcount.exception.BusinessException;
import com.fullcount.exception.ErrorCode;
import com.fullcount.mapper.CrewParticipantMapper;
import com.fullcount.mapper.PostMapper;
import com.fullcount.repository.MemberRepository;
import com.fullcount.repository.PostRepository;
import com.fullcount.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;

    @Transactional
    public PostDto.PostResponse createPost(Long authorId, PostDto.CreatePostRequest req) {
        Member author = memberRepository.findByIdWithTeam(authorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Post post = PostMapper.toEntity(req, author);

        if (req instanceof PostDto.CreateMateRequest mateReq) {
            Team homeTeam = findTeam(mateReq.getHomeTeamId());
            Team awayTeam = findTeam(mateReq.getAwayTeamId());
            post.setTeams(homeTeam, awayTeam);
            post.setStadium(homeTeam.getHomeStadium());

            if (author.getTeam() == null) {
                throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
            }
            post.setSupportTeam(author.getTeam());
        } else if (req instanceof PostDto.CreateCrewRequest crewReq) {
            Team supportTeam = findTeam(crewReq.getSupportTeamId());
            post.setSupportTeam(supportTeam);
            post.getParticipants().add(CrewParticipantMapper.toEntity(post, author, true, null, true));
        } else if (req instanceof PostDto.CreateTransferRequest transferReq) {
            if (transferReq.getTicketPrice() < 0) {
                throw new BusinessException(ErrorCode.TICKET_PRICE_EXCEEDED);
            }
            Team homeTeam = findTeam(transferReq.getHomeTeamId());
            Team awayTeam = findTeam(transferReq.getAwayTeamId());
            post.setTeams(homeTeam, awayTeam);
        }

        return PostMapper.toResponse(postRepository.save(post));
    }

    private Team findTeam(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
        }

        try {
            Long id = Long.parseLong(identifier);
            return teamRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        } catch (NumberFormatException e) {
            return teamRepository.findByShortName(identifier)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        }
    }

    @Transactional(readOnly = true)
    public List<PostDto.CrewMemberResponse> getCrewMembers(Long postId) {
        // findByIdWithAll: participants까지 fetch하므로 추가 쿼리 없음
        Post post = findPostWithAll(postId);
        if (post.getBoardType() != BoardType.CREW && post.getBoardType() != BoardType.MATE) {
            throw new BusinessException(ErrorCode.INVALID_BOARD_TYPE);
        }

        return post.getParticipants().stream()
                .map(CrewParticipantMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 크루 신청
    @Transactional
    public void joinCrew(Long postId, Long memberId) {
        joinParticipant(postId, memberId, null, BoardType.CREW);
    }

    // 메이트 신청
    @Transactional
    public PostDto.CrewMemberResponse joinMate(Long postId, Long memberId, PostDto.JoinMateRequest req) {
        return joinParticipant(postId, memberId, req, BoardType.MATE);
    }

    private PostDto.CrewMemberResponse joinParticipant(Long postId, Long memberId, PostDto.JoinMateRequest req, BoardType expectedBoardType) {
        // participants 순회(중복·인원 체크) → participants fetch join 전용 쿼리 사용
        Post post = findPostWithParticipants(postId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (post.getStatus() != PostStatus.OPEN) {
            throw new BusinessException(ErrorCode.POST_NOT_EDITABLE);
        }

        boolean alreadyParticipating = post.getParticipants().stream()
                .anyMatch(p -> p.getMember().getId().equals(memberId));
        if (alreadyParticipating) {
            throw new BusinessException(ErrorCode.ALREADY_PARTICIPATING);
        }

        long currentApprovedCount = post.getParticipants().stream()
                .filter(p -> p.getIsApproved() == null || p.getIsApproved())
                .count();

        if (currentApprovedCount >= post.getMaxParticipants()) {
            throw new BusinessException(ErrorCode.CREW_FULL);
        }

        boolean isApproved = true;
        if (expectedBoardType == BoardType.CREW && Boolean.FALSE.equals(post.getIsPublic())) {
            isApproved = false; // 비공개 글은 승인 대기!
        }



        CrewParticipant participant = CrewParticipantMapper.toEntity(
                post, member, false, req != null ? req.getApplyMessage() : null, isApproved
        );

        post.getParticipants().add(participant);
        Post savedPost = postRepository.save(post);
        CrewParticipant savedParticipant = savedPost.getParticipants().stream()
                .filter(p -> p.getMember().getId().equals(memberId))
                .findFirst()
                .orElse(participant);
        return CrewParticipantMapper.toResponse(savedParticipant);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PostDto.PostResponse> getPosts(BoardType boardType, String teamIdStr, PostStatus status, Pageable pageable) {
        Long teamId = null;
        if (teamIdStr != null && !teamIdStr.isBlank()) {
            teamId = findTeam(teamIdStr).getId();
        }
        Page<PostDto.PostResponse> page = postRepository.findByFilters(boardType, teamId, status, pageable)
                .map(PostMapper::toResponse);
        return PagedResponse.of(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PostDto.PostResponse> getTeamPosts(String teamIdStr, Pageable pageable) {
        Long teamId = findTeam(teamIdStr).getId();
        Page<PostDto.PostResponse> page = postRepository.findTeamOnlyByTeamId(teamId, pageable)
                .map(PostMapper::toResponse);
        return PagedResponse.of(page);
    }

    @Transactional
    public PostDto.PostResponse getPost(Long postId) {
        // findByIdWithAll: participants, 연관 팀 모두 fetch → 추가 쿼리 없음
        Post post = findPostWithAll(postId);
        post.incrementViewCount(); // 조회수+1
        return PostMapper.toResponse(post);
    }

    @Transactional
    public PostDto.PostResponse updatePost(Long postId, Long memberId, PostDto.UpdatePostRequest req) {
        // author 권한 체크만 필요 → findByIdWithAuthor 사용
        Post post = findPostWithAuthor(postId);

        if (!post.getAuthor().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        post.updateContent(req.getTitle(), req.getContent());
        return PostMapper.toResponse(post);
    }

    @Transactional
    public void deletePost(Long postId, Long memberId) {
        // author 권한 체크만 필요 → findByIdWithAuthor 사용
        Post post = findPostWithAuthor(postId);

        if (!post.getAuthor().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        if (!post.isEditable()) {
            throw new BusinessException(ErrorCode.POST_NOT_EDITABLE);
        }

        postRepository.delete(post);
    }

    @Transactional
    public void approveCrewMember(Long postId, Long targetMemberId, Long hostId) {
        Post post = findPostWithParticipants(postId);
        if (!post.getAuthor().getId().equals(hostId)) throw new BusinessException(ErrorCode.ACCESS_DENIED);

        CrewParticipant participant = post.getParticipants().stream()
                .filter(p -> p.getMember().getId().equals(targetMemberId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 정원 초과 검사 (승인된 사람만 셈)
        long approvedCount = post.getParticipants().stream().filter(p -> p.getIsApproved() != null && p.getIsApproved()).count();
        if (approvedCount >= post.getMaxParticipants()) throw new BusinessException(ErrorCode.CREW_FULL);

        participant.approve(); // 승인 상태로 변경 (목록에 포함됨)
    }

    // 방장의 크루원 거절 메서드
    @Transactional
    public void rejectCrewMember(Long postId, Long targetMemberId, Long hostId) {
        Post post = findPostWithParticipants(postId);
        if (!post.getAuthor().getId().equals(hostId)) throw new BusinessException(ErrorCode.ACCESS_DENIED);

        CrewParticipant participant = post.getParticipants().stream()
                .filter(p -> p.getMember().getId().equals(targetMemberId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 거절 시 참여자 명단에서 완전히 삭제 (이후 프론트엔드에서 알림/UI 처리)
        post.getParticipants().remove(participant);
    }

    // ── 내부 조회 헬퍼 ──────────────────────────────────────────────────────────

    /**
     * author만 fetch (권한 체크용: updatePost, deletePost)
     * - author lazy 로딩 방지
     */
    private Post findPostWithAuthor(Long postId) {
        return postRepository.findByIdWithAuthor(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    /**
     * 전체 fetch (상세 조회, getCrewMembers용)
     * - author, team, homeTeam, awayTeam, supportTeam, participants(+member) 모두 포함
     */
    private Post findPostWithAll(Long postId) {
        return postRepository.findByIdWithAll(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    /**
     * participants fetch (joinCrew용)
     * - participants 순회, member 접근 N+1 방지
     */
    private Post findPostWithParticipants(Long postId) {
        return postRepository.findByIdWithParticipants(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public PagedResponse<PostDto.PostResponse> getPosts(BoardType boardType, String teamIdStr, PostStatus status, boolean participating, Long memberId, Pageable pageable) {

        // 참여 중인 목록 요청 시 처리
        if (participating && memberId != null) {
            Page<PostDto.PostResponse> page = postRepository.findParticipatingPosts(boardType, memberId, pageable)
                    .map(PostMapper::toResponse);
            return PagedResponse.of(page);
        }

        Long teamId = null;
        if (teamIdStr != null && !teamIdStr.isBlank()) {
            teamId = findTeam(teamIdStr).getId();
        }
        Page<PostDto.PostResponse> page = postRepository.findByFilters(boardType, teamId, status, pageable)
                .map(PostMapper::toResponse);
        return PagedResponse.of(page);
    }
}
