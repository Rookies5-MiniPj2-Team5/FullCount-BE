package com.fullcount.repository;

import com.fullcount.domain.Transfer;
import com.fullcount.domain.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    @Query("SELECT t FROM Transfer t JOIN FETCH t.post JOIN FETCH t.seller LEFT JOIN FETCH t.buyer WHERE t.id = :id")
    Optional<Transfer> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT t FROM Transfer t JOIN FETCH t.post JOIN FETCH t.seller LEFT JOIN FETCH t.buyer WHERE t.seller.id = :sellerId")
    List<Transfer> findBySellerId(@Param("sellerId") Long sellerId);

    @Query("SELECT t FROM Transfer t JOIN FETCH t.post JOIN FETCH t.seller JOIN FETCH t.buyer WHERE t.buyer.id = :buyerId")
    List<Transfer> findByBuyerId(@Param("buyerId") Long buyerId);

    @Query("SELECT t FROM Transfer t JOIN FETCH t.post JOIN FETCH t.seller LEFT JOIN FETCH t.buyer WHERE t.status = :status")
    List<Transfer> findByStatus(@Param("status") TransferStatus status);

    @Query("SELECT t FROM Transfer t JOIN FETCH t.post JOIN FETCH t.seller LEFT JOIN FETCH t.buyer WHERE t.post.id = :postId")
    Optional<Transfer> findByPostId(@Param("postId") Long postId);

    boolean existsByPostId(@Param("postId") Long postId);
}
