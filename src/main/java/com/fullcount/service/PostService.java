package com.fullcount.service;

import com.fullcount.domain.*;
import com.fullcount.dto.PostDto;
import com.fullcount.dto.common.PagedResponse;
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
    private final TeamRepository teamRepository;

    @Transactional
    public PostDto.PostResponse createPost(Long authorId, PostDto.CreatePostRequest req) {
        Member author = memberRepository.findByIdWithTeam(authorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (req.getBoardType() == BoardType.TRANSFER
                && req.getTicketPrice() != null
                && req.getTicketPrice() < 0) {
            throw new BusinessException(ErrorCode.TICKET_PRICE_EXCEEDED);
        }

        Team homeTeam = req.getHomeTeamId() != null
                ? teamRepository.findById(req.getHomeTeamId()).orElse(null) : null;
        Team awayTeam = req.getAwayTeamId() != null
                ? teamRepository.findById(req.getAwayTeamId()).orElse(null) : null;

        Post post = PostMapper.toEntity(req, author, homeTeam, awayTeam);

        return PostMapper.toResponse(postRepository.save(post));
    }

    @Transactional(readOnly = true)
    public PagedResponse<PostDto.PostResponse> getPosts(BoardType boardType, Pageable pageable) {
        Page<PostDto.PostResponse> page = postRepository.findByBoardType(boardType, pageable)
                .map(PostMapper::toResponse);
        return PagedResponse.of(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PostDto.PostResponse> getTeamPosts(Long teamId, Pageable pageable) {
        Page<PostDto.PostResponse> page = postRepository.findTeamOnlyByTeamId(teamId, pageable)
                .map(PostMapper::toResponse);
        return PagedResponse.of(page);
    }

    @Transactional
    public PostDto.PostResponse getPost(Long postId) {
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
