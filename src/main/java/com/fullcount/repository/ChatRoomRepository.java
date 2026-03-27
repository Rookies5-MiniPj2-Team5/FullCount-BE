package com.fullcount.repository;

import com.fullcount.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT c FROM ChatRoom c JOIN FETCH c.post WHERE c.post.id = :postId")
    Optional<ChatRoom> findByPostId(Long postId);
}
