package com.fullcount.repository;

import com.fullcount.domain.Member;
import com.fullcount.domain.MemberRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findByNickname(String nickname);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    @Query("SELECT m FROM Member m JOIN FETCH m.team WHERE m.id = :id")
    Optional<Member> findByIdWithTeam(Long id);

    @Query("SELECT COUNT(m) AS totalCount, " +
            "SUM(CASE WHEN m.isActive = true THEN 1 ELSE 0 END) AS activeCount, " +
            "SUM(CASE WHEN m.isActive = false THEN 1 ELSE 0 END) AS inactiveCount, " +
            "SUM(CASE WHEN m.role = com.fullcount.domain.MemberRole.ADMIN THEN 1 ELSE 0 END) AS adminCount " +
            "FROM Member m")
    MemberDashboardSummary fetchDashboardSummary();

    @Query(value = "SELECT m.id FROM Member m " +
            "WHERE (:keyword IS NULL OR LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:active IS NULL OR m.isActive = :active) " +
            "AND (:role IS NULL OR m.role = :role) " +
            "ORDER BY m.createdAt DESC",
            countQuery = "SELECT COUNT(m) FROM Member m " +
                    "WHERE (:keyword IS NULL OR LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "   OR LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                    "AND (:active IS NULL OR m.isActive = :active) " +
                    "AND (:role IS NULL OR m.role = :role)")
    Page<Long> searchIdsForAdmin(@Param("keyword") String keyword,
                                 @Param("active") Boolean active,
                                 @Param("role") MemberRole role,
                                 Pageable pageable);

    @Query("SELECT m FROM Member m " +
            "LEFT JOIN FETCH m.team " +
            "WHERE m.id IN :ids")
    List<Member> findAllWithTeamByIdIn(@Param("ids") List<Long> ids);

    @Query("SELECT m FROM Member m " +
            "LEFT JOIN FETCH m.team " +
            "ORDER BY m.createdAt DESC")
    List<Member> findRecentForAdmin(Pageable pageable);

    interface MemberDashboardSummary {
        long getTotalCount();
        long getActiveCount();
        long getInactiveCount();
        long getAdminCount();
    }
}
