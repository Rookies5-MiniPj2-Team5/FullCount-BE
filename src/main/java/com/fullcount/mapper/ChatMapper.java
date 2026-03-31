package com.fullcount.mapper;

import com.fullcount.domain.ChatMessage;
import com.fullcount.domain.ChatRoom;
import com.fullcount.domain.Member;
import com.fullcount.dto.ChatDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ChatMapper {

    // Payload -> Entity 변환
    public static ChatMessage toEntity(ChatDTO.ChatMessagePayload payload, ChatRoom chatRoom, Member sender) {
        if (payload == null) return null;

        return ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(payload.getContent().trim())
                .build();
    }

    // Entity -> Payload 변환 (응답용)
    public static ChatDTO.ChatMessagePayload toPayload(ChatMessage message) {
        if (message == null) return null;

        return ChatDTO.ChatMessagePayload.builder()
                .messageId(message.getId())
                .senderId(message.getSender().getId())
                .senderNickname(message.getSender().getNickname())
                .content(message.getContent())
                .timestamp(extractTimestamp(message))
                .build();
    }

    private static String extractTimestamp(ChatMessage message) {
        if (message.getCreatedAt() != null) {
            return message.getCreatedAt().toString();
        }
        return LocalDateTime.now().toString();
    }

    // ChatRoom 리스트 항목 변환
    public static ChatDTO.ChatRoomResponse toChatRoomResponse(ChatRoom room, ChatMessage lastMessage) {
        return ChatDTO.ChatRoomResponse.builder()
                .chatRoomId(room.getId())
                .type(room.getRoomType())
                .title(room.getPost().getTitle())
                .lastMessage(lastMessage != null ? lastMessage.getContent() : null)
                .lastMessageAt(lastMessage != null ? extractTimestamp(lastMessage) : null)
                .unreadCount(0) // 미구현 (0으로 초기화)
                .build();
    }

    // ChatRoom 상세 응답 변환
    public static ChatDTO.ChatRoomDetailResponse toChatRoomDetailResponse(ChatRoom room, List<Member> participants) {
        List<ChatDTO.ParticipantResponse> participantDtos = participants.stream()
                .map(m -> ChatDTO.ParticipantResponse.builder()
                        .memberId(m.getId())
                        .nickname(m.getNickname())
                        .build())
                .collect(Collectors.toList());

        return ChatDTO.ChatRoomDetailResponse.builder()
                .chatRoomId(room.getId())
                .type(room.getRoomType())
                .participants(participantDtos)
                .build();
    }

    public static ChatDTO.ChatRoomResponse toChatRoomResponse(Long chatRoomId) {
        return ChatDTO.ChatRoomResponse.builder()
                .chatRoomId(chatRoomId)
                .build();
    }
}
