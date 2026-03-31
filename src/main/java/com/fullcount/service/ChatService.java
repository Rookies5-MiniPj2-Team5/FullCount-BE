package com.fullcount.service;

import com.fullcount.domain.*;
import com.fullcount.dto.ChatDTO;
import com.fullcount.exception.BusinessException;
import com.fullcount.exception.ErrorCode;
import com.fullcount.mapper.ChatMapper;
import com.fullcount.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;

    // 채팅 메시지 저장
    @Transactional
    public ChatDTO.ChatMessagePayload saveMessage(Long roomId, ChatDTO.ChatMessagePayload payload) {
        log.info("채팅 메시지 저장 시작: roomId={}, senderId={}", roomId, payload.getSenderId());

        validatePayload(payload);

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        Member sender = memberRepository.findById(payload.getSenderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        ChatMessage message = ChatMapper.toEntity(payload, chatRoom, sender);
        ChatMessage savedMessage = chatMessageRepository.save(message);

        log.info("채팅 메시지 저장 성공: messageId={}", savedMessage.getId());

        return ChatMapper.toPayload(savedMessage);
    }

    private void validatePayload(ChatDTO.ChatMessagePayload payload) {
        if (payload == null || payload.getSenderId() == null ||
                payload.getContent() == null || payload.getContent().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
