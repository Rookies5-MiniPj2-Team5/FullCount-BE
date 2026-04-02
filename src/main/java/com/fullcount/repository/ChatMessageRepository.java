package com.fullcount.repository;

import com.fullcount.domain.ChatMessage;
import com.fullcount.domain.Member;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m JOIN FETCH m.sender WHERE m.chatRoom.id = :roomId ORDER BY m.createdAt ASC")
    List<ChatMessage> findByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT m FROM ChatMessage m " +
            "JOIN FETCH m.sender " +
            "WHERE m.chatRoom.id = :roomId " +
            "AND (:lastId IS NULL OR m.id < :lastId) " +
            "ORDER BY m.id DESC")
    Slice<ChatMessage> findByRoomIdWithCursor(@Param("roomId") Long roomId, @Param("lastId") Long lastId, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m " +
            "JOIN FETCH m.sender " +
            "WHERE m.chatRoom.id IN :roomIds " +
            "AND m.id = (" +
            "  SELECT MAX(m2.id) FROM ChatMessage m2 WHERE m2.chatRoom.id = m.chatRoom.id" +
            ")")
    List<ChatMessage> findLastMessagesByRoomIds(@Param("roomIds") List<Long> roomIds);

    // 참여자 추출 전용
    @Query("SELECT DISTINCT m.sender FROM ChatMessage m WHERE m.chatRoom.id = :roomId")
    List<Member> findDistinctSendersByRoomId(@Param("roomId") Long roomId);
}
