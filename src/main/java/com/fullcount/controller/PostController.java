package com.fullcount.controller;

import com.fullcount.domain.BoardType;
import com.fullcount.domain.PostStatus;
import com.fullcount.dto.PostDto;
import com.fullcount.dto.common.PagedResponse;
import com.fullcount.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Post", description = "게시글 API")
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth") // 모든 메서드에 토큰 자물쇠 적용
public class PostController {

    private final PostService postService;

    @Operation(summary = "게시글 목록 조회 (게시글 타입, 팀 필터, 모집 상태 필터 포함)")
    @GetMapping
    public ResponseEntity<PagedResponse<PostDto.PostResponse>> getPosts(
            @RequestParam(defaultValue = "CREW") BoardType boardType,
            @RequestParam(required = false) String teamId, // 팀 선택 탭
            @RequestParam(required = false) PostStatus status, // 모집 중 / 마감 필터
            @ParameterObject @PageableDefault(size = 9, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(postService.getPosts(boardType, teamId, status, pageable));
    }

    @Operation(summary = "팀 전용 게시글 목록")
    @GetMapping("/team/{teamId}")
    public ResponseEntity<PagedResponse<PostDto.PostResponse>> getTeamPosts(
            @PathVariable String teamId,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(postService.getTeamPosts(teamId, pageable));
    }

    @Operation(summary = "게시글 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<PostDto.PostResponse> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPost(id));
    }

    @Operation(summary = "크루 참여 멤버 조회")
    @GetMapping("/{id}/members")
    public ResponseEntity<List<PostDto.CrewMemberResponse>> getCrewMembers(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getCrewMembers(id));
    }

    @Operation(summary = "크루 참여 신청")
    @PostMapping("/{id}/join")
    public ResponseEntity<Void> joinCrew(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId) {
        postService.joinCrew(id, memberId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "게시글 작성")
    @PostMapping
    public ResponseEntity<PostDto.PostResponse> createPost(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody PostDto.CreatePostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(memberId, request));
    }

    @Operation(summary = "게시글 수정 (OPEN 상태만 가능)")
    @PutMapping("/{id}")
    public ResponseEntity<PostDto.PostResponse> updatePost(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody PostDto.UpdatePostRequest req) {
        return ResponseEntity.ok(postService.updatePost(id, memberId, req));
    }

    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId) {
        postService.deletePost(id, memberId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "직관 메이트 참여 멤버 조회")
    @GetMapping("/{id}/mate/members")
    public ResponseEntity<List<PostDto.CrewMemberResponse>> getMateMembers(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getCrewMembers(id));
    }

    @Operation(summary = "직관 메이트 참여 신청")
    @PostMapping("/{id}/mate/join")
    public ResponseEntity<PostDto.CrewMemberResponse> joinMate(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody PostDto.JoinMateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.joinMate(id, memberId, request));
    }
}
