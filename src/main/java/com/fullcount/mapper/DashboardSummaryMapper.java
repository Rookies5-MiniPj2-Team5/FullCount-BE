package com.fullcount.mapper;

import com.fullcount.dto.admin.DashboardSummary;
import com.fullcount.repository.AdminDashboardRepository;

public class DashboardSummaryMapper {

    public static DashboardSummary toDashboardSummary(AdminDashboardRepository.DashboardSummaryProjection summary) {
        return DashboardSummary.builder()
                .memberCount(summary.getMemberCount())
                .activeMemberCount(summary.getActiveMemberCount())
                .inactiveMemberCount(summary.getInactiveMemberCount())
                .adminCount(summary.getAdminCount())
                .postCount(summary.getPostCount())
                .openPostCount(summary.getOpenPostCount())
                .reservedPostCount(summary.getReservedPostCount())
                .closedPostCount(summary.getClosedPostCount())
                .transferCount(summary.getTransferCount())
                .pendingTransferCount(summary.getPendingTransferCount())
                .completedTransferCount(summary.getCompletedTransferCount())
                .cancelledTransferCount(summary.getCancelledTransferCount())
                .build();
    }
}
