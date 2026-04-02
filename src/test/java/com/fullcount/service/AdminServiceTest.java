package com.fullcount.service;

import com.fullcount.domain.BoardType;
import com.fullcount.domain.Member;
import com.fullcount.domain.MemberRole;
import com.fullcount.domain.Post;
import com.fullcount.domain.PostStatus;
import com.fullcount.domain.Transfer;
import com.fullcount.domain.TransferStatus;
import com.fullcount.dto.admin.DashboardSummary;
import com.fullcount.exception.BusinessException;
import com.fullcount.exception.ErrorCode;
import com.fullcount.repository.AdminDashboardRepository;
import com.fullcount.repository.AttendanceRepository;
import com.fullcount.repository.ChatMessageRepository;
import com.fullcount.repository.ChatRoomRepository;
import com.fullcount.repository.MemberRepository;
import com.fullcount.repository.PostRepository;
import com.fullcount.repository.RefreshTokenRepository;
import com.fullcount.repository.TransferRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private AdminDashboardRepository adminDashboardRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private AdminQuerySupport adminQuerySupport;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("대시보드 요약은 회원, 게시글, 양도 집계를 하나의 응답으로 합친다")
    void getDashboardSummary_aggregatesRepositorySummaries() {
        when(adminDashboardRepository.fetchDashboardSummary())
                .thenReturn(dashboardSummary(10, 8, 2, 1, 9, 5, 2, 2, 7, 4, 2, 1));

        DashboardSummary result = adminService.getDashboardSummary();

        assertThat(result.getMemberCount()).isEqualTo(10);
        assertThat(result.getPostCount()).isEqualTo(9);
        assertThat(result.getTransferCount()).isEqualTo(7);
        assertThat(result.getPendingTransferCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("회원 목록 조회는 검색어를 trim하고 ID 페이지 순서를 유지한다")
    void getMembers_trimsKeywordAndRestoresPageOrder() {
        PageRequest pageable = PageRequest.of(0, 20);
        Member firstMember = member(1L, "one@test.com");
        Member secondMember = member(2L, "two@test.com");
        List<Member> unorderedMembers = List.of(firstMember, secondMember);
        List<Member> orderedMembers = List.of(secondMember, firstMember);

        when(adminQuerySupport.normalizeKeyword("  admin  ")).thenReturn("admin");
        when(memberRepository.searchIdsForAdmin("admin", true, MemberRole.ADMIN, pageable))
                .thenReturn(new PageImpl<>(List.of(2L, 1L), pageable, 2));
        when(memberRepository.findAllWithTeamByIdIn(List.of(2L, 1L))).thenReturn(unorderedMembers);
        when(adminQuerySupport.sortByPageOrder(unorderedMembers, List.of(2L, 1L))).thenReturn(orderedMembers);

        List<Member> result = adminService.getMembers("  admin  ", true, MemberRole.ADMIN, pageable).getContent();

        assertThat(result).extracting(Member::getId).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("게시글 목록 조회는 공백 검색어를 null로 정규화해서 조회한다")
    void getPosts_usesNullKeywordForBlankInput() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(adminQuerySupport.normalizeKeyword("   ")).thenReturn(null);
        when(postRepository.searchIdsForAdmin(null, BoardType.TRANSFER, PostStatus.OPEN, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        when(postRepository.findAllForAdminByIdIn(List.of())).thenReturn(List.of());
        when(adminQuerySupport.sortByPageOrder(List.of(), List.of())).thenReturn(List.of());

        adminService.getPosts("   ", BoardType.TRANSFER, PostStatus.OPEN, pageable);

        verify(postRepository).searchIdsForAdmin(isNull(), eq(BoardType.TRANSFER), eq(PostStatus.OPEN), eq(pageable));
    }

    @Test
    @DisplayName("대시보드 최신 회원 조회는 전용 최근 조회 쿼리로 3건만 가져온다")
    void getRecentMembersForDashboard_loadsThreeMembersWithoutExtraProcessing() {
        List<Member> members = List.of(member(1L, "one@test.com"), member(2L, "two@test.com"), member(3L, "three@test.com"));
        when(memberRepository.findRecentForAdmin(PageRequest.of(0, 3))).thenReturn(members);

        List<Member> result = adminService.getRecentMembersForDashboard();

        assertThat(result).hasSize(3);
        verify(memberRepository).findRecentForAdmin(PageRequest.of(0, 3));
    }

    @Test
    @DisplayName("대시보드 최신 게시글 조회는 전용 최근 조회 쿼리로 3건만 가져온다")
    void getRecentPostsForDashboard_loadsThreePostsWithoutExtraProcessing() {
        List<Post> posts = List.of(post(1L, PostStatus.OPEN), post(2L, PostStatus.OPEN), post(3L, PostStatus.CLOSED));
        when(postRepository.findRecentForAdmin(PageRequest.of(0, 3))).thenReturn(posts);

        List<Post> result = adminService.getRecentPostsForDashboard();

        assertThat(result).hasSize(3);
        verify(postRepository).findRecentForAdmin(PageRequest.of(0, 3));
    }

    @Test
    @DisplayName("대시보드 최신 거래 조회는 전용 최근 조회 쿼리로 3건만 가져온다")
    void getRecentTransfersForDashboard_loadsThreeTransfersWithoutExtraProcessing() {
        List<Transfer> transfers = List.of(
                transfer(1L, TransferStatus.REQUESTED, member(1L, "seller1@test.com"), null, post(1L, PostStatus.OPEN), 1000),
                transfer(2L, TransferStatus.PAYMENT_COMPLETED, member(2L, "seller2@test.com"), member(12L, "buyer2@test.com"), post(2L, PostStatus.OPEN), 2000),
                transfer(3L, TransferStatus.COMPLETED, member(3L, "seller3@test.com"), member(13L, "buyer3@test.com"), post(3L, PostStatus.CLOSED), 3000)
        );
        when(transferRepository.findRecentForAdmin(PageRequest.of(0, 3))).thenReturn(transfers);

        List<Transfer> result = adminService.getRecentTransfersForDashboard();

        assertThat(result).hasSize(3);
        verify(transferRepository).findRecentForAdmin(PageRequest.of(0, 3));
    }

    @Test
    @DisplayName("관리자는 자기 자신의 계정을 비활성화할 수 없다")
    void deactivateMember_rejectsSelfAction() {
        Member member = member(1L, "admin@test.com");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> adminService.deactivateMember(1L, "admin@test.com"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        assertThat(member.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("연관 데이터가 있는 회원은 삭제하지 않고 예외를 던진다")
    void deleteMember_withRelatedData_throwsAndDoesNotDelete() {
        Member member = member(3L, "user@test.com");
        when(memberRepository.findById(3L)).thenReturn(Optional.of(member));
        when(postRepository.existsByAuthorId(3L)).thenReturn(true);

        assertThatThrownBy(() -> adminService.deleteMember(3L, "admin@test.com"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        verify(refreshTokenRepository, never()).deleteByMemberId(3L);
        verify(memberRepository, never()).delete(member);
    }

    @Test
    @DisplayName("연관 데이터가 없는 회원 삭제 시 리프레시 토큰과 회원 엔티티를 함께 삭제한다")
    void deleteMember_withoutRelatedData_deletesRefreshTokensAndMember() {
        Member member = member(4L, "user@test.com");
        when(memberRepository.findById(4L)).thenReturn(Optional.of(member));
        when(postRepository.existsByAuthorId(4L)).thenReturn(false);
        when(transferRepository.existsBySellerId(4L)).thenReturn(false);
        when(transferRepository.existsByBuyerId(4L)).thenReturn(false);
        when(chatMessageRepository.existsBySenderId(4L)).thenReturn(false);
        when(attendanceRepository.existsByMemberId(4L)).thenReturn(false);

        adminService.deleteMember(4L, "admin@test.com");

        verify(refreshTokenRepository).deleteByMemberId(4L);
        verify(memberRepository).delete(member);
    }

    @Test
    @DisplayName("거래가 연결된 게시글은 관리자도 삭제할 수 없다")
    void deletePost_whenTransferExists_throws() {
        Post post = post(5L, PostStatus.OPEN);
        when(postRepository.findByIdWithAuthor(5L)).thenReturn(Optional.of(post));
        when(transferRepository.existsByPostId(5L)).thenReturn(true);

        assertThatThrownBy(() -> adminService.deletePost(5L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_EDITABLE);
    }

    @Test
    @DisplayName("티켓 전달 처리는 결제 완료 상태의 거래에서만 가능하다")
    void markTransferTicketSent_requiresPaymentCompleted() {
        Transfer transfer = transfer(6L, TransferStatus.REQUESTED, member(10L, "seller@test.com"), null, post(20L, PostStatus.OPEN), 1000);
        when(transferRepository.findByIdWithDetails(6L)).thenReturn(Optional.of(transfer));

        assertThatThrownBy(() -> adminService.markTransferTicketSent(6L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TRANSFER_INVALID_STATUS);
    }

    @Test
    @DisplayName("거래 완료 처리 시 판매자 정산과 게시글 마감이 함께 수행된다")
    void completeTransfer_completesTransferChargesSellerAndClosesPost() {
        Member seller = member(11L, "seller@test.com");
        Member buyer = member(12L, "buyer@test.com");
        Post post = post(21L, PostStatus.OPEN);
        Transfer transfer = transfer(7L, TransferStatus.PAYMENT_COMPLETED, seller, buyer, post, 5000);
        when(transferRepository.findByIdWithDetails(7L)).thenReturn(Optional.of(transfer));

        adminService.completeTransfer(7L);

        assertThat(seller.getBalance()).isEqualTo(5000);
        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(post.getStatus()).isEqualTo(PostStatus.CLOSED);
    }

    @Test
    @DisplayName("거래 취소 처리 시 구매자 환불과 게시글 마감이 함께 수행된다")
    void cancelTransfer_refundsBuyerAndClosesPost() {
        Member seller = member(13L, "seller@test.com");
        Member buyer = member(14L, "buyer@test.com");
        Post post = post(22L, PostStatus.OPEN);
        Transfer transfer = transfer(8L, TransferStatus.PAYMENT_COMPLETED, seller, buyer, post, 7000);
        when(transferRepository.findByIdWithDetails(8L)).thenReturn(Optional.of(transfer));

        adminService.cancelTransfer(8L);

        assertThat(buyer.getBalance()).isEqualTo(7000);
        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.CANCELLED);
        assertThat(post.getStatus()).isEqualTo(PostStatus.CLOSED);
    }

    private Member member(Long id, String email) {
        return Member.builder()
                .id(id)
                .email(email)
                .nickname("user" + id)
                .password("pw")
                .isActive(true)
                .build();
    }

    private Post post(Long id, PostStatus status) {
        return Post.builder()
                .id(id)
                .author(member(100L + id, "author" + id + "@test.com"))
                .boardType(BoardType.TRANSFER)
                .title("title-" + id)
                .content("content-" + id)
                .status(status)
                .build();
    }

    private Transfer transfer(Long id, TransferStatus status, Member seller, Member buyer, Post post, int price) {
        return Transfer.builder()
                .id(id)
                .seller(seller)
                .buyer(buyer)
                .post(post)
                .price(price)
                .status(status)
                .build();
    }

    private AdminDashboardRepository.DashboardSummaryProjection dashboardSummary(
            long memberCount,
            long activeMemberCount,
            long inactiveMemberCount,
            long adminCount,
            long postCount,
            long openPostCount,
            long reservedPostCount,
            long closedPostCount,
            long transferCount,
            long pendingTransferCount,
            long completedTransferCount,
            long cancelledTransferCount
    ) {
        return new AdminDashboardRepository.DashboardSummaryProjection() {
            @Override
            public long getMemberCount() {
                return memberCount;
            }

            @Override
            public long getActiveMemberCount() {
                return activeMemberCount;
            }

            @Override
            public long getInactiveMemberCount() {
                return inactiveMemberCount;
            }

            @Override
            public long getAdminCount() {
                return adminCount;
            }

            @Override
            public long getPostCount() {
                return postCount;
            }

            @Override
            public long getOpenPostCount() {
                return openPostCount;
            }

            @Override
            public long getReservedPostCount() {
                return reservedPostCount;
            }

            @Override
            public long getClosedPostCount() {
                return closedPostCount;
            }

            @Override
            public long getTransferCount() {
                return transferCount;
            }

            @Override
            public long getPendingTransferCount() {
                return pendingTransferCount;
            }

            @Override
            public long getCompletedTransferCount() {
                return completedTransferCount;
            }

            @Override
            public long getCancelledTransferCount() {
                return cancelledTransferCount;
            }
        };
    }
}
