package com.fullcount.repository;

import com.fullcount.domain.BoardType;
import com.fullcount.domain.Post;
import com.fullcount.domain.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    boolean existsByAuthorId(Long authorId);

    /**
     * 게시판 타입별 게시글 목록 (팀, 상태 필터 추가)
     * - CREW 타입 응답 시 participants.size() 호출 → participants fetch join 추가
     * - DISTINCT + Page 함께 쓸 경우 countQuery 분리 필수
     */
    @Query(value = "SELECT DISTINCT p FROM Post p " +
            "JOIN FETCH p.author a " +
            "LEFT JOIN FETCH p.homeTeam ht " +
            "LEFT JOIN FETCH p.awayTeam at " +
            "LEFT JOIN FETCH p.supportTeam st " +
            "LEFT JOIN FETCH p.participants pc " +
            "WHERE p.boardType = :boardType " +
            "AND (:teamId IS NULL OR p.supportTeam.id = :teamId OR p.homeTeam.id = :teamId OR p.awayTeam.id = :teamId) " +
            "AND (:status IS NULL OR p.status = :status) " +
            "ORDER BY p.createdAt DESC",
            countQuery = "SELECT COUNT(DISTINCT p) FROM Post p " +
                    "WHERE p.boardType = :boardType " +
                    "AND (:teamId IS NULL OR p.supportTeam.id = :teamId OR p.homeTeam.id = :teamId OR p.awayTeam.id = :teamId) " +
                    "AND (:status IS NULL OR p.status = :status)")
    Page<Post> findByFilters(@Param("boardType") BoardType boardType,
                             @Param("teamId") Long teamId,
                             @Param("status") PostStatus status,
                             Pageable pageable);

    /**
     * 게시판 타입별 게시글 목록 (N+1 방지: author, team Fetch Join)
     */
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.author a LEFT JOIN FETCH a.team " +
            "WHERE p.boardType = :boardType",
            countQuery = "SELECT count(p) FROM Post p WHERE p.boardType = :boardType")
    Page<Post> findByBoardType(@Param("boardType") BoardType boardType, Pageable pageable);

    /**
     * 팀 전용 게시판 - 팀 ID 필터
     */
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.author a JOIN FETCH a.team t " +
            "WHERE p.boardType = 'TEAM_ONLY' AND t.id = :teamId",
            countQuery = "SELECT count(p) FROM Post p JOIN p.author a JOIN a.team t " +
                    "WHERE p.boardType = 'TEAM_ONLY' AND t.id = :teamId")
    Page<Post> findTeamOnlyByTeamId(@Param("teamId") Long teamId, Pageable pageable);

    /**
     * 제목 + 내용 검색
     */
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.author " +
            "WHERE p.boardType = :boardType AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)",
            countQuery = "SELECT count(p) FROM Post p " +
                    "WHERE p.boardType = :boardType AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)")
    Page<Post> searchByKeyword(@Param("boardType") BoardType boardType,
                               @Param("keyword") String keyword,
                               Pageable pageable);

    /**
     * author fetch join (updatePost, deletePost에서 author 권한 체크용)
     */
    @Query("SELECT p FROM Post p JOIN FETCH p.author WHERE p.id = :postId")
    Optional<Post> findByIdWithAuthor(@Param("postId") Long postId);

    /**
     * 단건 상세 조회 (getPost용)
     * - author, team, 모든 연관 팀, participants 한 번에 fetch
     * - 이름과 실제 fetch 대상 일치하도록 participants 추가
     */
    @Query("SELECT p FROM Post p " +
            "JOIN FETCH p.author a " +
            "LEFT JOIN FETCH p.team " +
            "LEFT JOIN FETCH p.homeTeam " +
            "LEFT JOIN FETCH p.awayTeam " +
            "LEFT JOIN FETCH p.supportTeam " +
            "LEFT JOIN FETCH p.participants pc " +
            "LEFT JOIN FETCH pc.member " +
            "WHERE p.id = :id")
    Optional<Post> findByIdWithAll(@Param("id") Long id);

    /**
     * joinCrew 전용: participants, author, supportTeam 함께 fetch
     * - participants 순회(중복 체크, 인원 체크) + author 비교 없이 participants만 필요
     */
    @Query("SELECT p FROM Post p " +
            "JOIN FETCH p.author " +
            "LEFT JOIN FETCH p.supportTeam " +
            "LEFT JOIN FETCH p.participants pc " +
            "LEFT JOIN FETCH pc.member " +
            "WHERE p.id = :postId")
    Optional<Post> findByIdWithParticipants(@Param("postId") Long postId);

    @Query(value = "SELECT p.id FROM Post p " +
            "JOIN p.author a " +
            "WHERE (:boardType IS NULL OR p.boardType = :boardType) " +
            "AND (:status IS NULL OR p.status = :status) " +
            "AND (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(a.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY p.createdAt DESC",
            countQuery = "SELECT COUNT(p) FROM Post p " +
                    "JOIN p.author a " +
                    "WHERE (:boardType IS NULL OR p.boardType = :boardType) " +
                    "AND (:status IS NULL OR p.status = :status) " +
                    "AND (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "   OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "   OR LOWER(a.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Long> searchIdsForAdmin(@Param("keyword") String keyword,
                                 @Param("boardType") BoardType boardType,
                                 @Param("status") PostStatus status,
                                 Pageable pageable);

    @Query("SELECT p FROM Post p " +
            "JOIN FETCH p.author a " +
            "LEFT JOIN FETCH a.team " +
            "LEFT JOIN FETCH p.homeTeam " +
            "LEFT JOIN FETCH p.awayTeam " +
            "LEFT JOIN FETCH p.supportTeam " +
            "WHERE p.id IN :ids")
    List<Post> findAllForAdminByIdIn(@Param("ids") List<Long> ids);

    @Query("SELECT p FROM Post p " +
            "JOIN FETCH p.author a " +
            "LEFT JOIN FETCH a.team " +
            "LEFT JOIN FETCH p.homeTeam " +
            "LEFT JOIN FETCH p.awayTeam " +
            "LEFT JOIN FETCH p.supportTeam " +
            "ORDER BY p.createdAt DESC")
    List<Post> findRecentForAdmin(Pageable pageable);

    @Query("SELECT COUNT(p) AS totalCount, " +
            "SUM(CASE WHEN p.status = com.fullcount.domain.PostStatus.OPEN THEN 1 ELSE 0 END) AS openCount, " +
            "SUM(CASE WHEN p.status = com.fullcount.domain.PostStatus.RESERVED THEN 1 ELSE 0 END) AS reservedCount, " +
            "SUM(CASE WHEN p.status = com.fullcount.domain.PostStatus.CLOSED THEN 1 ELSE 0 END) AS closedCount " +
            "FROM Post p")
    PostDashboardSummary fetchDashboardSummary();

    interface PostDashboardSummary {
        long getTotalCount();
        long getOpenCount();
        long getReservedCount();
        long getClosedCount();
    }

    List<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    @Query("SELECT DISTINCT p FROM Post p JOIN p.participants pc WHERE pc.member.id = :memberId ORDER BY p.createdAt DESC")
    List<Post> findPostsByParticipantId(@Param("memberId") Long memberId);
}
