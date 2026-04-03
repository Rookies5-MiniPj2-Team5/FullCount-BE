package com.fullcount.repository;

import com.fullcount.domain.ChatReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatReadStatusRepository extends JpaRepository<ChatReadStatus, Long> {

    Optional<ChatReadStatus> findByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);

    // 특정 채팅방에서 lastReadMessageId 이후 메시지 수 조회
    @Query("SELECT COUNT(m) FROM ChatMessage m " +
            "WHERE m.chatRoom.id = :roomId " +
            "AND (:lastReadId IS NULL OR m.id > :lastReadId)")
    int countUnreadMessages(@Param("roomId") Long roomId, @Param("lastReadId") Long lastReadId);

    // 여러 채팅방의 읽음 상태 한번에 조회
    @Query("SELECT s FROM ChatReadStatus s " +
            "WHERE s.member.id = :memberId " +
            "AND s.chatRoom.id IN :roomIds")
    List<ChatReadStatus> findByMemberIdAndRoomIds(
            @Param("memberId") Long memberId,
            @Param("roomIds") List<Long> roomIds);
}