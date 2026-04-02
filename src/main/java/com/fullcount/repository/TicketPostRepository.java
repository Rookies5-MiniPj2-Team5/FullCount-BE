package com.fullcount.repository;

import com.fullcount.domain.TicketPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketPostRepository extends JpaRepository<TicketPost, Long>, TicketPostRepositoryCustom {
}
