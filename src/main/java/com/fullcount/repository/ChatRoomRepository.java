package com.fullcount.repository;

import com.fullcount.domain.ChatRoom;
import com.fullcount.domain.ChatRoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT c FROM ChatRoom c JOIN FETCH c.post WHERE c.post.id = :postId")
    Optional<ChatRoom> findByPostId(@Param("postId") Long postId);

    /**
     * 두 유저 간 기존 직접 DM 방 조회 (중복 생성 방지).
     * 발신·수신 방향 무관하게 탐색합니다.
     */
    @Query("SELECT c FROM ChatRoom c " +
            "WHERE c.roomType = :type " +
            "AND ((c.initiator.id = :userAId AND c.receiver.id = :userBId) " +
            "  OR (c.initiator.id = :userBId AND c.receiver.id = :userAId))")
    Optional<ChatRoom> findDirectDmBetween(
            @Param("userAId") Long userAId,
            @Param("userBId") Long userBId,
            @Param("type") ChatRoomType type);

    /**
     * 내 채팅방 목록 조회.
     * - 게시글 기반 방(ONE_ON_ONE, GROUP_*): 작성자 또는 양도 구매자
     * - 직접 DM 방(ONE_ON_ONE_DIRECT): initiator 또는 receiver
     */
    @Query(value = "SELECT DISTINCT c FROM ChatRoom c " +
            "LEFT JOIN FETCH c.post p " +
            "LEFT JOIN FETCH p.author a " +
            "WHERE (c.roomType <> com.fullcount.domain.ChatRoomType.ONE_ON_ONE_DIRECT " +
            "  AND (a.id = :memberId OR EXISTS (SELECT t FROM Transfer t WHERE t.post.id = p.id AND t.buyer.id = :memberId))) " +
            "OR (c.roomType = com.fullcount.domain.ChatRoomType.ONE_ON_ONE_DIRECT " +
            "  AND (c.initiator.id = :memberId OR c.receiver.id = :memberId))",
            countQuery = "SELECT COUNT(DISTINCT c) FROM ChatRoom c " +
                    "LEFT JOIN c.post p " +
                    "WHERE (c.roomType <> com.fullcount.domain.ChatRoomType.ONE_ON_ONE_DIRECT " +
                    "  AND (p.author.id = :memberId OR EXISTS (SELECT t FROM Transfer t WHERE t.post.id = p.id AND t.buyer.id = :memberId))) " +
                    "OR (c.roomType = com.fullcount.domain.ChatRoomType.ONE_ON_ONE_DIRECT " +
                    "  AND (c.initiator.id = :memberId OR c.receiver.id = :memberId))")
    Page<ChatRoom> findAllByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    @Query("SELECT DISTINCT c FROM ChatRoom c " +
            "JOIN FETCH c.post p " +
            "JOIN FETCH p.author a " +
            "LEFT JOIN FETCH c.messages msg " +
            "LEFT JOIN FETCH msg.sender " +
            "WHERE c.id = :roomId")
    Optional<ChatRoom> findByIdWithDetails(@Param("roomId") Long roomId);

    /**
     * 채팅방 참여 권한 체크.
     * - ONE_ON_ONE_DIRECT: initiator 또는 receiver
     * - 그 외: 게시글 작성자, 양도 구매자, 또는 메시지 발신자
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
            "FROM ChatRoom c " +
            "LEFT JOIN c.post p " +
            "WHERE c.id = :roomId " +
            "AND (c.roomType = com.fullcount.domain.ChatRoomType.ONE_ON_ONE_DIRECT " +
            "       AND (c.initiator.id = :memberId OR c.receiver.id = :memberId) " +
            "  OR (c.roomType <> com.fullcount.domain.ChatRoomType.ONE_ON_ONE_DIRECT " +
            "       AND (p.author.id = :memberId " +
            "        OR EXISTS (SELECT t FROM Transfer t WHERE t.post.id = p.id AND t.buyer.id = :memberId) " +
            "        OR EXISTS (SELECT m FROM ChatMessage m WHERE m.chatRoom.id = :roomId AND m.sender.id = :memberId))))")
    boolean isParticipant(@Param("roomId") Long roomId, @Param("memberId") Long memberId);
}
