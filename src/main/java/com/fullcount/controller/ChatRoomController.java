package com.fullcount.controller;

import com.fullcount.domain.ChatRoomType;
import com.fullcount.dto.ChatDTO;
import com.fullcount.dto.common.CursorResponse;
import com.fullcount.dto.common.PagedResponse;
import com.fullcount.mapper.ChatMapper;
import com.fullcount.service.ChatRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@Slf4j
@Tag(name = "Chat", description = "채팅 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @Operation(summary = "내 채팅방 목록 조회", description = "로그인한 회원이 참여 중인 채팅방 목록을 페이징하여 조회합니다.")
    @GetMapping
    public ResponseEntity<PagedResponse<ChatDTO.ChatRoomResponse>> getMyChatRooms(
            @AuthenticationPrincipal Long memberId,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(chatRoomService.getMyChatRooms(memberId, pageable));
    }

    @Operation(summary = "그룹 채팅방 생성 (티켓 양도 채팅방 생성은 양도 요청 API 실행)", description = "동행/크루 모집 게시글에 대한 그룹 채팅방 생성 (티켓 양도 채팅방 생성은 양도 요청 API 실행)")
    @PostMapping
    public ResponseEntity<ChatDTO.ChatRoomResponse> createGroupChatRoom(
            @AuthenticationPrincipal Long memberId,
            @RequestParam Long postId,
            @RequestParam ChatRoomType type) {

        Long roomId = chatRoomService.createChatRoom(memberId, postId, type);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ChatMapper.toChatRoomResponse(roomId));
    }

    @Operation(
            summary = "유저 간 1:1 DM 채팅방 생성 (티켓 연락하기)",
            description = "targetUserId를 경로 변수로 받아 발신자와의 1:1 DM 방을 생성하거나 기존 방을 반환합니다. 중복 생성을 방지합니다.")
    @PostMapping("/dm/user/{targetUserId}")
    public ResponseEntity<ChatDTO.ChatRoomResponse> createDirectDm(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long targetUserId) {

        Long roomId = chatRoomService.createOrFindDirectDm(memberId, targetUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ChatMapper.toChatRoomResponse(roomId));
    }

    @Operation(summary = "채팅방 상세 조회", description = "특정 채팅방의 정보와 참여자 목록을 조회합니다.")
    @GetMapping("/{roomId}")
    public ResponseEntity<ChatDTO.ChatRoomDetailResponse> getChatRoomDetail(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long roomId) {
        return ResponseEntity.ok(chatRoomService.getChatRoomDetail(memberId, roomId));
    }

    @Operation(summary = "채팅 메시지 내역 조회", description = "커서 기반 페이징을 사용하여 특정 채팅방의 이전 메시지 내역을 조회합니다.")
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<CursorResponse<ChatDTO.ChatMessagePayload>> getChatMessages(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long roomId,
            @RequestParam(required = false) Long lastMessageId,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(chatRoomService.getChatMessages(memberId, roomId, lastMessageId, size));
    }
}
