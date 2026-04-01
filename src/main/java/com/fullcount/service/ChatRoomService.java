package com.fullcount.service;

import com.fullcount.domain.*;
import com.fullcount.dto.ChatDTO;
import com.fullcount.dto.common.CursorResponse;
import com.fullcount.dto.common.PagedResponse;
import com.fullcount.exception.BusinessException;
import com.fullcount.exception.ErrorCode;
import com.fullcount.mapper.ChatMapper;
import com.fullcount.mapper.ChatRoomMapper;
import com.fullcount.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PostRepository postRepository;
    private final TransferRepository transferRepository;

    // 내 채팅방 목록 조회 (Pageable 적용 -> PagedResponse 반환)
    public PagedResponse<ChatDTO.ChatRoomResponse> getMyChatRooms(Long memberId, Pageable pageable) {
        log.info("채팅방 목록 조회 시작 - memberId={}, page={}, size={}", memberId, pageable.getPageNumber(), pageable.getPageSize());

        Page<ChatRoom> chatRooms = chatRoomRepository.findAllByMemberId(memberId, pageable);

        // 채팅방 ID 목록으로 최신 메시지를 한 번에 조회 (N+1 방지)
        List<Long> roomIds = chatRooms.map(ChatRoom::getId).toList();
        Map<Long, ChatMessage> lastMessageMap = chatMessageRepository
                .findLastMessagesByRoomIds(roomIds)
                .stream()
                .collect(Collectors.toMap(m -> m.getChatRoom().getId(), m -> m));

        Page<ChatDTO.ChatRoomResponse> responsePage = chatRooms.map(room ->
                ChatMapper.toChatRoomResponse(room, lastMessageMap.get(room.getId())));

        log.info("채팅방 목록 조회 완료 - memberId={}, 총 채팅방 수={}", memberId, chatRooms.getTotalElements());
        return PagedResponse.of(responsePage);
    }

    // 채팅방 상세 조회
    public ChatDTO.ChatRoomDetailResponse getChatRoomDetail(Long memberId, Long roomId) {
        log.info("채팅방 상세 조회 시작 - memberId={}, roomId={}", memberId, roomId);

        ChatRoom chatRoom = chatRoomRepository.findByIdWithDetails(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        List<Member> participants = getParticipants(chatRoom);

        // 권한 체크
        boolean isParticipant = participants.stream()
                .anyMatch(m -> m.getId().equals(memberId));
        if (!isParticipant) {
            log.warn("채팅방 상세 조회 접근 거부 - memberId={}, roomId={}", memberId, roomId);
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        log.info("채팅방 상세 조회 완료 - memberId={}, roomId={}, 참여자 수={}", memberId, roomId, participants.size());
        return ChatMapper.toChatRoomDetailResponse(chatRoom, participants);
    }

    // 채팅 메시지 내역 조회 (커서 기반 무한 스크롤 - CursorResponse 반환)
    public CursorResponse<ChatDTO.ChatMessagePayload> getChatMessages(Long memberId, Long roomId, Long lastMessageId, int size) {
        log.info("채팅 메시지 내역 조회 시작 - memberId={}, roomId={}, lastMessageId={}, size={}", memberId, roomId, lastMessageId, size);

        // 채팅방 존재 여부 확인
        if (!chatRoomRepository.existsById(roomId)) {
            log.warn("채팅 메시지 내역 조회 실패 - 채팅방 없음, roomId={}", roomId);
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        // 참여자 권한 체크 (경량 쿼리)
        if (!chatRoomRepository.isParticipant(roomId, memberId)) {
            log.warn("채팅 메시지 내역 조회 접근 거부 - memberId={}, roomId={}", memberId, roomId);
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 커서 페이징은 항상 0번 페이지를 조회
        PageRequest pageRequest = PageRequest.of(0, size);
        Slice<ChatMessage> messageSlice = chatMessageRepository
                .findByRoomIdWithCursor(roomId, lastMessageId, pageRequest);

        List<ChatDTO.ChatMessagePayload> content = messageSlice.getContent().stream()
                .map(ChatMapper::toPayload)
                .collect(Collectors.toList());

        Long nextCursor = null;
        if (!content.isEmpty()) {
            nextCursor = content.get(content.size() - 1).getMessageId();
        }

        log.info("채팅 메시지 내역 조회 완료 - memberId={}, roomId={}, 조회 메시지 수={}, hasNext={}, nextCursor={}",
                memberId, roomId, content.size(), messageSlice.hasNext(), nextCursor);

        return CursorResponse.<ChatDTO.ChatMessagePayload>builder()
                .content(content)
                .nextCursor(nextCursor)
                .hasNext(messageSlice.hasNext())
                .build();
    }

    private List<Member> getParticipants(ChatRoom chatRoom) {
        log.debug("채팅방 참여자 조회 시작 - roomId={}, roomType={}", chatRoom.getId(), chatRoom.getRoomType());

        // 중복 제거를 위해 Set 사용
        Set<Member> participants = new HashSet<>();

        // 방장은 항상 포함
        participants.add(chatRoom.getPost().getAuthor());

        if (chatRoom.getRoomType() == ChatRoomType.ONE_ON_ONE) {
            // 1:1 채팅: 양도 데이터에서 구매자 확인
            transferRepository.findByPostId(chatRoom.getPost().getId())
                    .ifPresent(t -> { if (t.getBuyer() != null) participants.add(t.getBuyer()); });
        } else {
            // 그룹 채팅: messages 컬렉션 대신 전용 쿼리로 sender 추출 (N+1 방지)
            participants.addAll(chatMessageRepository.findDistinctSendersByRoomId(chatRoom.getId()));
        }

        log.debug("채팅방 참여자 조회 완료 - roomId={}, 참여자 수={}", chatRoom.getId(), participants.size());
        return new ArrayList<>(participants);
    }

    // 그룹 채팅방 생성
    @Transactional
    public Long createChatRoom(Long memberId, Long postId, ChatRoomType chatRoomType) {
        log.info("채팅방 생성 시작 - memberId={}, postId={}, chatRoomType={}", memberId, postId, chatRoomType);

        Post post = postRepository.findByIdWithAuthor(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.getAuthor().getId().equals(memberId)) {
            log.warn("채팅방 생성 접근 거부 - memberId={}, postId={}", memberId, postId);
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        Long roomId = chatRoomRepository.findByPostId(postId)
                .map(ChatRoom::getId)
                .orElseGet(() -> {
                    ChatRoom newRoom = ChatRoomMapper.toEntity(post, chatRoomType);
                    Long newRoomId = chatRoomRepository.save(newRoom).getId();
                    log.info("새 채팅방 생성 완료 - roomId={}", newRoomId);
                    return newRoomId;
                });

        log.info("채팅방 생성 완료 - memberId={}, postId={}, roomId={}", memberId, postId, roomId);
        return roomId;
    }
}