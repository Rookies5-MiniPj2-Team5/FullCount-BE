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
        Page<ChatRoom> chatRooms = chatRoomRepository.findAllByMemberId(memberId, pageable);

        Page<ChatDTO.ChatRoomResponse> responsePage = chatRooms.map(room -> {
            ChatMessage lastMessage = room.getMessages().stream()
                    .max(Comparator.comparing(ChatMessage::getCreatedAt))
                    .orElse(null);
            return ChatMapper.toChatRoomResponse(room, lastMessage);
        });

        return PagedResponse.of(responsePage);
    }

    // 채팅방 상세 조회
    public ChatDTO.ChatRoomDetailResponse getChatRoomDetail(Long memberId, Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findByIdWithDetails(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        List<Member> participants = getParticipants(chatRoom);

        // 권한 체크
        boolean isParticipant = participants.stream()
                .anyMatch(m -> m.getId().equals(memberId));
        if (!isParticipant) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        return ChatMapper.toChatRoomDetailResponse(chatRoom, participants);
    }

    // 채팅 메시지 내역 조회 (커서 기반 무한 스크롤 - CursorResponse 반환)
    public CursorResponse<ChatDTO.ChatMessagePayload> getChatMessages(Long memberId, Long roomId, Long lastMessageId, int size) {
        ChatRoom chatRoom = chatRoomRepository.findByIdWithDetails(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 참여자 권한 체크
        List<Member> participants = getParticipants(chatRoom);
        boolean isParticipant = participants.stream()
                .anyMatch(m -> m.getId().equals(memberId));
        if (!isParticipant) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 커서 페이징은 항상 0번 페이지를 조회
        PageRequest pageRequest = PageRequest.of(0, size);
        Slice<ChatMessage> messageSlice = chatMessageRepository.findByRoomIdWithCursor(roomId, lastMessageId, pageRequest);

        List<ChatDTO.ChatMessagePayload> content = messageSlice.getContent().stream()
                .map(ChatMapper::toPayload)
                .collect(Collectors.toList());

        Long nextCursor = null;
        if (!content.isEmpty()) {
            nextCursor = content.get(content.size() - 1).getMessageId();
        }

        return CursorResponse.<ChatDTO.ChatMessagePayload>builder()
                .content(content)
                .nextCursor(nextCursor)
                .hasNext(messageSlice.hasNext())
                .build();
    }

    private List<Member> getParticipants(ChatRoom chatRoom) {
        // 중복 제거를 위해 Set 사용
        Set<Member> participants = new HashSet<>();

        // 1. 방장은 항상 포함
        participants.add(chatRoom.getPost().getAuthor());

        if (chatRoom.getRoomType() == ChatRoomType.ONE_ON_ONE) {
            // 1:1 채팅일 경우: 양도 데이터에서 구매자 확인
            transferRepository.findByPostId(chatRoom.getPost().getId())
                    .ifPresent(t -> {
                        if (t.getBuyer() != null) {
                            participants.add(t.getBuyer());
                        }
                    });
        } else {
            // 그룹 채팅(GROUP_JOIN, GROUP_CREW)일 경우:
            // 채팅 메시지 내역에서 보낸 사람들을 추출하여 추가 (Set이므로 자동 중복 제거)
            chatRoom.getMessages().stream()
                    .map(ChatMessage::getSender)
                    .forEach(participants::add);
        }

        // 다시 List로 변환하여 반환
        return new ArrayList<>(participants);
    }

    // 그룹 채팅방 생성
    @Transactional
    public Long createChatRoom(Long memberId, Long postId, ChatRoomType chatRoomType) {
        Post post = postRepository.findByIdWithAuthor(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.getAuthor().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        return chatRoomRepository.findByPostId(postId)
                .map(ChatRoom::getId)
                .orElseGet(() -> {
                    ChatRoom newRoom = ChatRoomMapper.toEntity(post, chatRoomType);
                    return chatRoomRepository.save(newRoom).getId();
                });
    }
}
