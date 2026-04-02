package com.fullcount.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
@AllArgsConstructor
public class DashboardSummary {
    private final long memberCount;
    private final long activeMemberCount;
    private final long inactiveMemberCount;
    private final long adminCount;
    private final long postCount;
    private final long openPostCount;
    private final long reservedPostCount;
    private final long closedPostCount;
    private final long transferCount;
    private final long pendingTransferCount;
    private final long completedTransferCount;
    private final long cancelledTransferCount;
}
