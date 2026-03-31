package com.fullcount.repository;

import com.fullcount.domain.BoardType;
import com.fullcount.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    /** 게시판 타입별 게시글 목록 (N+1 방지: author, team Fetch Join) */
    @Query("SELECT p FROM Post p JOIN FETCH p.author a LEFT JOIN FETCH a.team " +
           "WHERE p.boardType = :boardType ORDER BY p.createdAt DESC")
    Page<Post> findByBoardType(@Param("boardType") BoardType boardType, Pageable pageable);

    /** 팀 전용 게시판 - 팀 ID 필터 */
    @Query("SELECT p FROM Post p JOIN FETCH p.author a JOIN FETCH a.team t " +
           "WHERE p.boardType = 'TEAM_ONLY' AND t.id = :teamId ORDER BY p.createdAt DESC")
    Page<Post> findTeamOnlyByTeamId(@Param("teamId") Long teamId, Pageable pageable);

    /** 제목 + 내용 검색 */
    @Query("SELECT p FROM Post p JOIN FETCH p.author " +
           "WHERE p.boardType = :boardType AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)")
    Page<Post> searchByKeyword(@Param("boardType") BoardType boardType,
                               @Param("keyword") String keyword,
                               Pageable pageable);

    @Query("SELECT p FROM Post p JOIN FETCH p.author WHERE p.id = :postId")
    Optional<Post> findByIdWithAuthor(@Param("postId") Long postId);

    @Query("SELECT p FROM Post p JOIN FETCH p.author JOIN FETCH p.team WHERE p.id = :id")
    Optional<Post> findByIdWithAll(Long id);
}
