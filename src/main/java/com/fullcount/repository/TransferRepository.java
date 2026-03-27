package com.fullcount.repository;

import com.fullcount.domain.Transfer;
import com.fullcount.domain.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    @Query("SELECT t FROM Transfer t JOIN FETCH t.post JOIN FETCH t.seller WHERE t.id = :id")
    Optional<Transfer> findByIdWithDetails(Long id);

    List<Transfer> findBySellerId(Long sellerId);

    List<Transfer> findByBuyerId(Long buyerId);

    List<Transfer> findByStatus(TransferStatus status);

    boolean existsByPostId(@Param("postId") Long postId);
}
