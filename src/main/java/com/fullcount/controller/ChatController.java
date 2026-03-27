package com.fullcount.controller;

import com.fullcount.domain.ChatMessage;
import com.fullcount.domain.ChatRoom;
import com.fullcount.domain.Member;
import com.fullcount.exception.BusinessException;
import com.fullcount.exception.ErrorCode;
import com.fullcount.repository.ChatMessageRepository;
import com.fullcount.repository.ChatRoomRepository;
import com.fullcount.repository.MemberRepository;
import lombok.*;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;

    /**
     * 클라이언트 발행: /app/chat/{roomId}
     * 구독 경로:     /topic/chat/{roomId}
     */
    @MessageMapping("/chat/{roomId}")
    @SendTo("/topic/chat/{roomId}")
    public ChatMessagePayload sendMessage(
            @DestinationVariable Long roomId,
            ChatMessagePayload payload) {

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        Member sender = memberRepository.findById(payload.getSenderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(payload.getContent())
                .build();
        chatMessageRepository.save(message);

        payload.setSenderNickname(sender.getNickname());
        payload.setTimestamp(LocalDateTime.now().toString());
        return payload;
    }

    // ── 메시지 페이로드 ──
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessagePayload {
        private Long senderId;
        private String senderNickname;
        private String content;
        private String timestamp;
    }
}
