package com.fullcount.mapper;

import com.fullcount.domain.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatRoomMapper {

    /** 채팅방 공통 생성 */
    public static ChatRoom toEntity(ChatRoomType chatRoomType, Post post, Member initiator, Member receiver) {
        return ChatRoom.builder()
                .post(post)
                .roomType(chatRoomType)
                .initiator(initiator)
                .receiver(receiver)
                .build();
    }

    public static ChatRoom toEntity(ChatRoomType chatRoomType, TicketPost ticketPost, Member initiator, Member receiver) {
        return ChatRoom.builder()
                .ticketPost(ticketPost)
                .roomType(chatRoomType)
                .initiator(initiator)
                .receiver(receiver)
                .build();
    }
}
