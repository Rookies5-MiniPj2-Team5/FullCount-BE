package com.fullcount.repository;

import com.fullcount.domain.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT c FROM ChatRoom c JOIN FETCH c.post WHERE c.post.id = :postId")
    Optional<ChatRoom> findByPostId(@Param("postId") Long postId);

    @Query(value = "SELECT DISTINCT c FROM ChatRoom c " +
            "JOIN FETCH c.post p " +
            "JOIN FETCH p.author a " +
            "WHERE a.id = :memberId OR EXISTS (SELECT t FROM Transfer t WHERE t.post.id = p.id AND t.buyer.id = :memberId)",
            countQuery = "SELECT COUNT(DISTINCT c) FROM ChatRoom c " +
                    "JOIN c.post p " +
                    "WHERE p.author.id = :memberId OR EXISTS (SELECT t FROM Transfer t WHERE t.post.id = p.id AND t.buyer.id = :memberId)")
    Page<ChatRoom> findAllByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    @Query("SELECT DISTINCT c FROM ChatRoom c " +
            "JOIN FETCH c.post p " +
            "JOIN FETCH p.author a " +
            "LEFT JOIN FETCH c.messages msg " +
            "LEFT JOIN FETCH msg.sender " +
            "WHERE c.id = :roomId")
    Optional<ChatRoom> findByIdWithDetails(@Param("roomId") Long roomId);

    // ChatRoomRepository — 권한 체크 전용
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
            "FROM ChatRoom c " +
            "JOIN c.post p " +
            "WHERE c.id = :roomId " +
            "AND (p.author.id = :memberId " +
            "  OR EXISTS (SELECT t FROM Transfer t WHERE t.post.id = p.id AND t.buyer.id = :memberId)" +
            "  OR EXISTS (SELECT m FROM ChatMessage m WHERE m.chatRoom.id = :roomId AND m.sender.id = :memberId))")
    boolean isParticipant(@Param("roomId") Long roomId, @Param("memberId") Long memberId);
}
