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
            Team homeTeam = teamRepository.findById(mateReq.getHomeTeamId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
            Team awayTeam = teamRepository.findById(mateReq.getAwayTeamId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
            post.setTeams(homeTeam, awayTeam);
        } else if (req instanceof PostDto.CreateCrewRequest crewReq) {
            Team supportTeam = teamRepository.findById(crewReq.getSupportTeamId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
            post.setSupportTeam(supportTeam);
            post.getParticipants().add(CrewParticipantMapper.toEntity(post, author, true));
        } else if (req instanceof PostDto.CreateTransferRequest transferReq) {
            if (transferReq.getTicketPrice() < 0) {
                throw new BusinessException(ErrorCode.TICKET_PRICE_EXCEEDED);
            }
            Team homeTeam = teamRepository.findById(transferReq.getHomeTeamId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
            Team awayTeam = teamRepository.findById(transferReq.getAwayTeamId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
            post.setTeams(homeTeam, awayTeam);
        }

        return PostMapper.toResponse(postRepository.save(post));
    }

    @Transactional(readOnly = true)
    public List<PostDto.CrewMemberResponse> getCrewMembers(Long postId) {
        // findByIdWithAll: participants까지 fetch하므로 추가 쿼리 없음
        Post post = findPostWithAll(postId);
        if (post.getBoardType() != BoardType.CREW) {
            throw new BusinessException(ErrorCode.INVALID_BOARD_TYPE);
        }

        return post.getParticipants().stream()
                .map(CrewParticipantMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void joinCrew(Long postId, Long memberId) {
        // participants 순회(중복·인원 체크) → participants fetch join 전용 쿼리 사용
        Post post = findPostWithParticipants(postId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (post.getBoardType() != BoardType.CREW) {
            throw new BusinessException(ErrorCode.INVALID_BOARD_TYPE);
        }

        if (post.getStatus() != PostStatus.OPEN) {
            throw new BusinessException(ErrorCode.POST_NOT_EDITABLE);
        }

        boolean alreadyParticipating = post.getParticipants().stream()
                .anyMatch(p -> p.getMember().getId().equals(memberId));
        if (alreadyParticipating) {
            throw new BusinessException(ErrorCode.ALREADY_PARTICIPATING);
        }

        if (post.getParticipants().size() >= post.getMaxParticipants()) {
            throw new BusinessException(ErrorCode.CREW_FULL);
        }

        post.getParticipants().add(CrewParticipantMapper.toEntity(post, member, false));
    }

    @Transactional(readOnly = true)
    public PagedResponse<PostDto.PostResponse> getPosts(BoardType boardType, Long teamId, PostStatus status, Pageable pageable) {
        Page<PostDto.PostResponse> page = postRepository.findByFilters(boardType, teamId, status, pageable)
                .map(PostMapper::toResponse);
        return PagedResponse.of(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PostDto.PostResponse> getTeamPosts(Long teamId, Pageable pageable) {
        Page<PostDto.PostResponse> page = postRepository.findTeamOnlyByTeamId(teamId, pageable)
                .map(PostMapper::toResponse);
        return PagedResponse.of(page);
    }

    @Transactional(readOnly = true)
    public PostDto.PostResponse getPost(Long postId) {
        // findByIdWithAll: participants, 연관 팀 모두 fetch → 추가 쿼리 없음
        Post post = findPostWithAll(postId);
        post.incrementViewCount();
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
}