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
import com.fullcount.mapper.DashboardSummaryMapper;
import com.fullcount.repository.AdminDashboardRepository;
import com.fullcount.repository.AttendanceRepository;
import com.fullcount.repository.ChatMessageRepository;
import com.fullcount.repository.ChatRoomRepository;
import com.fullcount.repository.MemberRepository;
import com.fullcount.repository.PostRepository;
import com.fullcount.repository.RefreshTokenRepository;
import com.fullcount.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final TransferRepository transferRepository;
    private final AdminDashboardRepository adminDashboardRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AttendanceRepository attendanceRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final AdminQuerySupport adminQuerySupport;

    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary() {
        AdminDashboardRepository.DashboardSummaryProjection summaryProjection = adminDashboardRepository.fetchDashboardSummary();
        DashboardSummary summary = DashboardSummaryMapper.toDashboardSummary(summaryProjection);

        log.debug("관리자 대시보드 요약 조회 완료: {}", summary);
        return summary;
    }

    @Transactional(readOnly = true)
    public List<Member> getRecentMembersForDashboard() {
        List<Member> members = memberRepository.findRecentForAdmin(PageRequest.of(0, 3));
        log.debug("관리자 대시보드 최신 회원 조회 완료: count={}", members.size());
        return members;
    }

    @Transactional(readOnly = true)
    public List<Post> getRecentPostsForDashboard() {
        List<Post> posts = postRepository.findRecentForAdmin(PageRequest.of(0, 3));
        log.debug("관리자 대시보드 최신 게시글 조회 완료: count={}", posts.size());
        return posts;
    }

    @Transactional(readOnly = true)
    public List<Transfer> getRecentTransfersForDashboard() {
        List<Transfer> transfers = transferRepository.findRecentForAdmin(PageRequest.of(0, 3));
        log.debug("관리자 대시보드 최신 거래 조회 완료: count={}", transfers.size());
        return transfers;
    }

    @Transactional(readOnly = true)
    public Page<Member> getMembers(String keyword, Boolean active, MemberRole role, Pageable pageable) {
        String normalizedKeyword = adminQuerySupport.normalizeKeyword(keyword);
        Page<Long> idPage = memberRepository.searchIdsForAdmin(normalizedKeyword, active, role, pageable);
        List<Member> members = memberRepository.findAllWithTeamByIdIn(idPage.getContent());
        Page<Member> result = new PageImpl<>(
                adminQuerySupport.sortByPageOrder(members, idPage.getContent()),
                pageable,
                idPage.getTotalElements()
        );

        log.debug("관리자 회원 목록 조회 완료: keyword={}, active={}, role={}, page={}, size={}, totalElements={}",
                normalizedKeyword, active, role, pageable.getPageNumber(), pageable.getPageSize(), result.getTotalElements());
        return result;
    }

    @Transactional(readOnly = true)
    public Page<Post> getPosts(String keyword, BoardType boardType, PostStatus status, Pageable pageable) {
        String normalizedKeyword = adminQuerySupport.normalizeKeyword(keyword);
        Page<Long> idPage = postRepository.searchIdsForAdmin(normalizedKeyword, boardType, status, pageable);
        List<Post> posts = postRepository.findAllForAdminByIdIn(idPage.getContent());
        Page<Post> result = new PageImpl<>(
                adminQuerySupport.sortByPageOrder(posts, idPage.getContent()),
                pageable,
                idPage.getTotalElements()
        );

        log.debug("관리자 게시글 목록 조회 완료: keyword={}, boardType={}, status={}, page={}, size={}, totalElements={}",
                normalizedKeyword, boardType, status, pageable.getPageNumber(), pageable.getPageSize(), result.getTotalElements());
        return result;
    }

    @Transactional(readOnly = true)
    public Page<Transfer> getTransfers(String keyword, TransferStatus status, Pageable pageable) {
        String normalizedKeyword = adminQuerySupport.normalizeKeyword(keyword);
        Page<Long> idPage = transferRepository.searchIdsForAdmin(normalizedKeyword, status, pageable);
        List<Transfer> transfers = transferRepository.findAllForAdminByIdIn(idPage.getContent());
        Page<Transfer> result = new PageImpl<>(
                adminQuerySupport.sortByPageOrder(transfers, idPage.getContent()),
                pageable,
                idPage.getTotalElements()
        );

        log.debug("관리자 양도 목록 조회 완료: keyword={}, status={}, page={}, size={}, totalElements={}",
                normalizedKeyword, status, pageable.getPageNumber(), pageable.getPageSize(), result.getTotalElements());
        return result;
    }

    @Transactional
    public void deactivateMember(Long memberId, String currentAdminEmail) {
        Member member = getMember(memberId);
        validateSelfAction(member, currentAdminEmail, "본인 계정은 비활성화할 수 없습니다.");
        member.deactivate();
        log.info("관리자 회원 비활성화 완료: memberId={}, adminEmail={}", memberId, currentAdminEmail);
    }

    @Transactional
    public void activateMember(Long memberId) {
        getMember(memberId).activate();
        log.info("관리자 회원 활성화 완료: memberId={}", memberId);
    }

    @Transactional
    public void changeMemberRole(Long memberId, MemberRole role, String currentAdminEmail) {
        Member member = getMember(memberId);
        validateSelfAction(member, currentAdminEmail, "본인 권한은 변경할 수 없습니다.");
        member.changeRole(role);
        log.info("관리자 회원 권한 변경 완료: memberId={}, role={}, adminEmail={}", memberId, role, currentAdminEmail);
    }

    @Transactional
    public void deleteMember(Long memberId, String currentAdminEmail) {
        Member member = getMember(memberId);
        validateSelfAction(member, currentAdminEmail, "본인 계정은 삭제할 수 없습니다.");

        if (hasRelatedMemberData(memberId)) {
            throw new BusinessException(
                    ErrorCode.ACCESS_DENIED,
                    "연관 데이터가 있는 회원은 삭제할 수 없습니다. 비활성화 기능을 이용해주세요."
            );
        }

        refreshTokenRepository.deleteByMemberId(memberId);
        memberRepository.delete(member);
        log.info("관리자 회원 삭제 완료: memberId={}, adminEmail={}", memberId, currentAdminEmail);
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findByIdWithAuthor(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (transferRepository.existsByPostId(postId) || chatRoomRepository.findByPostId(postId).isPresent()) {
            throw new BusinessException(
                    ErrorCode.POST_NOT_EDITABLE,
                    "거래 또는 채팅 이력이 연결된 게시글은 삭제할 수 없습니다. 상태 관리로 처리하세요."
            );
        }

        postRepository.delete(post);
        log.info("관리자 게시글 삭제 완료: postId={}", postId);
    }

    @Transactional
    public void markTransferTicketSent(Long transferId) {
        Transfer transfer = getTransfer(transferId);
        if (transfer.getStatus() != TransferStatus.PAYMENT_COMPLETED) {
            throw new BusinessException(
                    ErrorCode.TRANSFER_INVALID_STATUS,
                    "결제 완료 상태의 거래만 티켓 전달 처리할 수 있습니다."
            );
        }

        transfer.markTicketSent();
        log.info("관리자 티켓 전달 처리 완료: transferId={}", transferId);
    }

    @Transactional
    public void completeTransfer(Long transferId) {
        Transfer transfer = getTransfer(transferId);

        if (transfer.getBuyer() == null || transfer.getStatus() == TransferStatus.REQUESTED) {
            throw new BusinessException(
                    ErrorCode.TRANSFER_INVALID_STATUS,
                    "구매자가 지정되고 결제가 진행된 거래만 완료 처리할 수 있습니다."
            );
        }
        if (transfer.getStatus() == TransferStatus.CANCELLED || transfer.getStatus() == TransferStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.TRANSFER_INVALID_STATUS, "이미 종료된 거래입니다.");
        }

        transfer.getSeller().charge(transfer.getPrice());
        transfer.confirmTransfer();
        transfer.getPost().close();

        log.info("관리자 거래 완료 처리 완료: transferId={}, sellerId={}, buyerId={}, amount={}",
                transferId, transfer.getSeller().getId(), transfer.getBuyer().getId(), transfer.getPrice());
    }

    @Transactional
    public void cancelTransfer(Long transferId) {
        Transfer transfer = getTransfer(transferId);

        if (transfer.getStatus() == TransferStatus.CANCELLED || transfer.getStatus() == TransferStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.TRANSFER_INVALID_STATUS, "이미 종료된 거래입니다.");
        }

        if (shouldRefundBuyer(transfer)) {
            transfer.getBuyer().charge(transfer.getPrice());
        }

        transfer.cancelTransfer();
        transfer.getPost().close();

        log.info("관리자 거래 취소 처리 완료: transferId={}, buyerRefunded={}, amount={}",
                transferId, transfer.getBuyer() != null, transfer.getPrice());
    }

    private boolean hasRelatedMemberData(Long memberId) {
        return postRepository.existsByAuthorId(memberId)
                || transferRepository.existsBySellerId(memberId)
                || transferRepository.existsByBuyerId(memberId)
                || chatMessageRepository.existsBySenderId(memberId)
                || attendanceRepository.existsByMemberId(memberId);
    }

    private boolean shouldRefundBuyer(Transfer transfer) {
        return (transfer.getStatus() == TransferStatus.PAYMENT_COMPLETED
                || transfer.getStatus() == TransferStatus.TICKET_SENT)
                && transfer.getBuyer() != null;
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private Transfer getTransfer(Long transferId) {
        return transferRepository.findByIdWithDetails(transferId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSFER_NOT_FOUND));
    }

    private void validateSelfAction(Member member, String currentAdminEmail, String message) {
        if (member.getEmail().equalsIgnoreCase(currentAdminEmail)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, message);
        }
    }
}
