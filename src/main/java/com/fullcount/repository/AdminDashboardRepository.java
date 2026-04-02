package com.fullcount.repository;

import com.fullcount.domain.Member;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface AdminDashboardRepository extends Repository<Member, Long> {

    @Query(value = """
            SELECT
                (SELECT COUNT(*) FROM member) AS memberCount,
                (SELECT COUNT(*) FROM member WHERE is_active = true) AS activeMemberCount,
                (SELECT COUNT(*) FROM member WHERE is_active = false) AS inactiveMemberCount,
                (SELECT COUNT(*) FROM member WHERE role = 'ADMIN') AS adminCount,
                (SELECT COUNT(*) FROM post) AS postCount,
                (SELECT COUNT(*) FROM post WHERE status = 'OPEN') AS openPostCount,
                (SELECT COUNT(*) FROM post WHERE status = 'RESERVED') AS reservedPostCount,
                (SELECT COUNT(*) FROM post WHERE status = 'CLOSED') AS closedPostCount,
                (SELECT COUNT(*) FROM transfer) AS transferCount,
                (SELECT COUNT(*) FROM transfer WHERE status IN ('REQUESTED', 'PAYMENT_COMPLETED', 'TICKET_SENT')) AS pendingTransferCount,
                (SELECT COUNT(*) FROM transfer WHERE status = 'COMPLETED') AS completedTransferCount,
                (SELECT COUNT(*) FROM transfer WHERE status = 'CANCELLED') AS cancelledTransferCount
            """, nativeQuery = true)
    DashboardSummaryProjection fetchDashboardSummary();

    interface DashboardSummaryProjection {
        long getMemberCount();
        long getActiveMemberCount();
        long getInactiveMemberCount();
        long getAdminCount();
        long getPostCount();
        long getOpenPostCount();
        long getReservedPostCount();
        long getClosedPostCount();
        long getTransferCount();
        long getPendingTransferCount();
        long getCompletedTransferCount();
        long getCancelledTransferCount();
    }
}
