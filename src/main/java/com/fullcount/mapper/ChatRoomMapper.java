package com.fullcount.mapper;

import com.fullcount.domain.*;
import org.springframework.stereotype.Component;

@Component
public class ChatRoomMapper {

    /** 1:1 채팅방 엔티티 생성 */
    public static ChatRoom toEntity(Post post, ChatRoomType chatRoomType) {
        return ChatRoom.builder()
                .post(post)
                .roomType(chatRoomType)
                .build();
    }
}