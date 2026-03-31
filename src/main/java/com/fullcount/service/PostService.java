package com.fullcount.service;

import com.fullcount.domain.*;
import com.fullcount.dto.PostDto;
import com.fullcount.exception.BusinessException;
import com.fullcount.exception.ErrorCode;
import com.fullcount.mapper.PostMapper;
import com.fullcount.repository.MemberRepository;
import com.fullcount.repository.PostRepository;
import com.fullcount.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository; // 추가

    @Transactional
    public PostDto.PostResponse createPost(Long authorId, PostDto.CreatePostRequest req) {
        Member author = memberRepository.findByIdWithTeam(authorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // TRANSFER 가격 정가 초과 검증 (필요 시 도메인 내부로 이동 가능)
        if (req.getBoardType() == BoardType.TRANSFER
                && req.getTicketPrice() != null
                && req.getTicketPrice() < 0) { // 예시 로직: 실제 비즈니스 규칙에 맞게 수정
            throw new BusinessException(ErrorCode.TICKET_PRICE_EXCEEDED);
        }

        // Team 엔티티 참조 조회 (DB 조회를 피하려면 getReferenceById 사용)
        Team homeTeam = req.getHomeTeamId() != null
                ? teamRepository.findById(req.getHomeTeamId()).orElse(null) : null;
        Team awayTeam = req.getAwayTeamId() != null
                ? teamRepository.findById(req.getAwayTeamId()).orElse(null) : null;

        Post post = PostMapper.toEntity(req, author, homeTeam, awayTeam);

        return PostMapper.toResponse(postRepository.save(post));
    }

    @Transactional(readOnly = true)
    public Page<PostDto.PostResponse> getPosts(BoardType boardType, Pageable pageable) {
        return postRepository.findByBoardType(boardType, pageable)
                .map(PostMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PostDto.PostResponse> getTeamPosts(Long teamId, Pageable pageable) {
        return postRepository.findTeamOnlyByTeamId(teamId, pageable)
                .map(PostMapper::toResponse);
    }

    @Transactional
    public PostDto.PostResponse getPost(Long postId) {
        // 단건 조회 시에도 Fetch Join 메서드 사용 권장
        Post post = postRepository.findByIdWithAll(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        post.incrementViewCount();
        return PostMapper.toResponse(post);
    }

    @Transactional
    public PostDto.PostResponse updatePost(Long postId, Long memberId, PostDto.UpdatePostRequest req) {
        Post post = findPost(postId);

        if (!post.getAuthor().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        post.updateContent(req.getTitle(), req.getContent());
        return PostMapper.toResponse(post);
    }

    @Transactional
    public void deletePost(Long postId, Long memberId) {
        Post post = findPost(postId);

        if (!post.getAuthor().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        if (!post.isEditable()) {
            throw new BusinessException(ErrorCode.POST_NOT_EDITABLE);
        }

        postRepository.delete(post);
    }

    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }
}