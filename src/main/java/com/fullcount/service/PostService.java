package com.fullcount.service;

import com.fullcount.domain.*;
import com.fullcount.dto.PostDto;
import com.fullcount.exception.BusinessException;
import com.fullcount.exception.ErrorCode;
import com.fullcount.repository.MemberRepository;
import com.fullcount.repository.PostRepository;
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

    @Transactional
    public PostDto.Response createPost(Long authorId, PostDto.CreateRequest req) {
        Member author = memberRepository.findByIdWithTeam(authorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // TRANSFER 가격 정가 초과 검증
        if (req.getBoardType() == BoardType.TRANSFER
                && req.getTicketPrice() != null
                && req.getTicketPrice() < 0) {
            throw new BusinessException(ErrorCode.TICKET_PRICE_EXCEEDED);
        }

        Team homeTeam = req.getHomeTeamId() != null 
                ? Team.builder().id(req.getHomeTeamId()).build() : null;
        Team awayTeam = req.getAwayTeamId() != null 
                ? Team.builder().id(req.getAwayTeamId()).build() : null;

        Post post = Post.builder()
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

        return PostDto.Response.from(postRepository.save(post));
    }

    @Transactional(readOnly = true)
    public Page<PostDto.Response> getPosts(BoardType boardType, Pageable pageable) {
        return postRepository.findByBoardType(boardType, pageable)
                .map(PostDto.Response::from);
    }

    @Transactional(readOnly = true)
    public Page<PostDto.Response> getTeamPosts(Long teamId, Pageable pageable) {
        return postRepository.findTeamOnlyByTeamId(teamId, pageable)
                .map(PostDto.Response::from);
    }

    @Transactional
    public PostDto.Response getPost(Long postId) {
        Post post = findPost(postId);
        post.incrementViewCount();
        return PostDto.Response.from(post);
    }

    @Transactional
    public PostDto.Response updatePost(Long postId, Long memberId, PostDto.UpdateRequest req) {
        Post post = findPost(postId);

        if (!post.getAuthor().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        post.updateContent(req.getTitle(), req.getContent());
        return PostDto.Response.from(post);
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
