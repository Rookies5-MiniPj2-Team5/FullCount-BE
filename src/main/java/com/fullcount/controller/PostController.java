package com.fullcount.controller;

import com.fullcount.domain.BoardType;
import com.fullcount.dto.PostDto;
import com.fullcount.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Post", description = "게시글 API")
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "게시글 목록 조회 (boardType 필터, 페이징)")
    @GetMapping
    public ResponseEntity<Page<PostDto.Response>> getPosts(
            @RequestParam(defaultValue = "GENERAL") BoardType boardType,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(postService.getPosts(boardType, pageable));
    }

    @Operation(summary = "팀 전용 게시글 목록")
    @GetMapping("/team/{teamId}")
    public ResponseEntity<Page<PostDto.Response>> getTeamPosts(
            @PathVariable Long teamId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(postService.getTeamPosts(teamId, pageable));
    }

    @Operation(summary = "게시글 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<PostDto.Response> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPost(id));
    }

    @Operation(summary = "게시글 작성")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<PostDto.Response> createPost(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody PostDto.CreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(memberId, req));
    }

    @Operation(summary = "게시글 수정 (OPEN 상태만 가능)")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public ResponseEntity<PostDto.Response> updatePost(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody PostDto.UpdateRequest req) {
        return ResponseEntity.ok(postService.updatePost(id, memberId, req));
    }

    @Operation(summary = "게시글 삭제")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId) {
        postService.deletePost(id, memberId);
        return ResponseEntity.noContent().build();
    }
}
