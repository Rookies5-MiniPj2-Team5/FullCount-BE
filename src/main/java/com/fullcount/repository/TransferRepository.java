package com.fullcount.repository;

import com.fullcount.domain.Transfer;
import com.fullcount.domain.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    boolean existsBySellerId(Long sellerId);
    boolean existsByBuyerId(Long buyerId);

    @Query("SELECT t FROM Transfer t LEFT JOIN FETCH t.post LEFT JOIN FETCH t.seller LEFT JOIN FETCH t.buyer WHERE t.id = :id")
    Optional<Transfer> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT t FROM Transfer t JOIN FETCH t.post JOIN FETCH t.seller LEFT JOIN FETCH t.buyer WHERE t.seller.id = :sellerId")
    List<Transfer> findBySellerId(@Param("sellerId") Long sellerId);

    @Query("SELECT t FROM Transfer t JOIN FETCH t.post JOIN FETCH t.seller JOIN FETCH t.buyer WHERE t.buyer.id = :buyerId")
    List<Transfer> findByBuyerId(@Param("buyerId") Long buyerId);

    @Query("SELECT t FROM Transfer t JOIN FETCH t.post JOIN FETCH t.seller LEFT JOIN FETCH t.buyer WHERE t.status = :status")
    List<Transfer> findByStatus(@Param("status") TransferStatus status);

    @Query("SELECT t FROM Transfer t JOIN FETCH t.post JOIN FETCH t.seller LEFT JOIN FETCH t.buyer WHERE t.post.id = :postId")
    Optional<Transfer> findByPostId(@Param("postId") Long postId);

    @Query("SELECT t FROM Transfer t " +
            "JOIN FETCH t.seller LEFT JOIN FETCH t.buyer " +
            "WHERE t.ticketPost.id = " +
            "(SELECT c.ticketPost.id FROM ChatRoom c WHERE c.id = :roomId AND c.ticketPost IS NOT NULL)")
    Optional<Transfer> findByRoomId(@Param("roomId") Long roomId);

    Optional<Transfer> findByTicketPostId(Long ticketPostId);

    boolean existsByPostId(@Param("postId") Long postId);
    boolean existsByTicketPostId(Long ticketPostId);

    @Query(value = "SELECT t.id FROM Transfer t " +
            "LEFT JOIN t.post p " +
            "LEFT JOIN t.ticketPost tp " +
            "JOIN t.seller s " +
            "LEFT JOIN t.buyer b " +
            "WHERE (:status IS NULL OR t.status = :status) " +
            "AND (:keyword IS NULL OR LOWER(COALESCE(p.title, tp.title)) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(s.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR (b IS NOT NULL AND LOWER(b.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
            "ORDER BY t.createdAt DESC",
            countQuery = "SELECT COUNT(t) FROM Transfer t " +
                    "LEFT JOIN t.post p " +
                    "LEFT JOIN t.ticketPost tp " +
                    "JOIN t.seller s " +
                    "LEFT JOIN t.buyer b " +
                    "WHERE (:status IS NULL OR t.status = :status) " +
                    "AND (:keyword IS NULL OR LOWER(COALESCE(p.title, tp.title)) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "   OR LOWER(s.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "   OR (b IS NOT NULL AND LOWER(b.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))))")
    Page<Long> searchIdsForAdmin(@Param("keyword") String keyword,
                                 @Param("status") TransferStatus status,
                                 Pageable pageable);

    @Query("SELECT t FROM Transfer t " +
            "LEFT JOIN FETCH t.post p " +
            "LEFT JOIN FETCH t.ticketPost tp " +
            "JOIN FETCH t.seller s " +
            "LEFT JOIN FETCH t.buyer b " +
            "WHERE t.id IN :ids")
    List<Transfer> findAllForAdminByIdIn(@Param("ids") List<Long> ids);

    @Query("SELECT t FROM Transfer t " +
            "LEFT JOIN FETCH t.post p " +
            "LEFT JOIN FETCH t.ticketPost tp " +
            "JOIN FETCH t.seller s " +
            "LEFT JOIN FETCH t.buyer b " +
            "ORDER BY t.createdAt DESC")
    List<Transfer> findRecentForAdmin(Pageable pageable);

    @Query("SELECT COUNT(t) AS totalCount, " +
            "SUM(CASE WHEN t.status IN (com.fullcount.domain.TransferStatus.REQUESTED, com.fullcount.domain.TransferStatus.PAYMENT_COMPLETED, com.fullcount.domain.TransferStatus.TICKET_SENT) THEN 1 ELSE 0 END) AS pendingCount, " +
            "SUM(CASE WHEN t.status = com.fullcount.domain.TransferStatus.COMPLETED THEN 1 ELSE 0 END) AS completedCount, " +
            "SUM(CASE WHEN t.status = com.fullcount.domain.TransferStatus.CANCELLED THEN 1 ELSE 0 END) AS cancelledCount " +
            "FROM Transfer t")
    TransferDashboardSummary fetchDashboardSummary();

    @Query("SELECT t FROM Transfer t " +
            "JOIN FETCH t.post p " +
            "WHERE t.buyer.id = :buyerId " +
            "ORDER BY t.id DESC")
    Page<Transfer> findAllByBuyerId(@Param("buyerId") Long buyerId, Pageable pageable);

    interface TransferDashboardSummary {
        long getTotalCount();
        long getPendingCount();
        long getCompletedCount();
        long getCancelledCount();
    }
}
