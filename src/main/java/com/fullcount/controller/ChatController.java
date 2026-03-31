package com.fullcount.controller;

import com.fullcount.dto.ChatDTO;
import com.fullcount.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 클라이언트 발행: /app/chat/{roomId}
     * 클라이언트 구독: /topic/chat/{roomId}
     */
    @MessageMapping("/chat/{roomId}")
    public void sendMessage(
            @DestinationVariable Long roomId,
            ChatDTO.ChatMessagePayload payload
    ) {
        ChatDTO.ChatMessagePayload response = chatService.saveMessage(roomId, payload);

        log.info("response={}", response);

        messagingTemplate.convertAndSend("/topic/chat/" + roomId, response);
    }
}