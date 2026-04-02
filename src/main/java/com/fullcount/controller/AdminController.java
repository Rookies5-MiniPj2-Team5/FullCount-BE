package com.fullcount.controller;

import com.fullcount.domain.BoardType;
import com.fullcount.domain.MemberRole;
import com.fullcount.domain.PostStatus;
import com.fullcount.domain.TransferStatus;
import com.fullcount.dto.admin.AdminMemberFilterForm;
import com.fullcount.dto.admin.AdminPostFilterForm;
import com.fullcount.dto.admin.AdminTransferFilterForm;
import com.fullcount.exception.BusinessException;
import com.fullcount.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @ModelAttribute("memberFilter")
    public AdminMemberFilterForm memberFilter() {
        return new AdminMemberFilterForm();
    }

    @ModelAttribute("postFilter")
    public AdminPostFilterForm postFilter() {
        return new AdminPostFilterForm();
    }

    @ModelAttribute("transferFilter")
    public AdminTransferFilterForm transferFilter() {
        return new AdminTransferFilterForm();
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("summary", adminService.getDashboardSummary());
        model.addAttribute("recentMembers", adminService.getRecentMembersForDashboard());
        model.addAttribute("recentPosts", adminService.getRecentPostsForDashboard());
        model.addAttribute("recentTransfers", adminService.getRecentTransfersForDashboard());
        return "admin/dashboard";
    }

    @GetMapping("/members")
    public String members(
            @Valid @ModelAttribute("memberFilter") AdminMemberFilterForm filter,
            BindingResult bindingResult,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Model model
    ) {
        Page<?> members = bindingResult.hasErrors()
                ? emptyPage(pageable)
                : adminService.getMembers(filter.getKeyword(), filter.getActive(), filter.getRole(), pageable);

        model.addAttribute("members", members);
        model.addAttribute("keyword", filter.getKeyword());
        model.addAttribute("active", filter.getActive());
        model.addAttribute("role", filter.getRole());
        model.addAttribute("roles", MemberRole.values());
        model.addAttribute("returnUrl", buildMembersReturnUrl(filter.getKeyword(), filter.getActive(), filter.getRole(), pageable.getPageNumber()));
        return "admin/members";
    }

    @GetMapping("/posts")
    public String posts(
            @Valid @ModelAttribute("postFilter") AdminPostFilterForm filter,
            BindingResult bindingResult,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Model model
    ) {
        Page<?> posts = bindingResult.hasErrors()
                ? emptyPage(pageable)
                : adminService.getPosts(filter.getKeyword(), filter.getBoardType(), filter.getStatus(), pageable);

        model.addAttribute("posts", posts);
        model.addAttribute("keyword", filter.getKeyword());
        model.addAttribute("boardType", filter.getBoardType());
        model.addAttribute("status", filter.getStatus());
        model.addAttribute("boardTypes", BoardType.values());
        model.addAttribute("postStatuses", PostStatus.values());
        model.addAttribute("returnUrl", buildPostsReturnUrl(filter.getKeyword(), filter.getBoardType(), filter.getStatus(), pageable.getPageNumber()));
        return "admin/posts";
    }

    @GetMapping("/transfers")
    public String transfers(
            @Valid @ModelAttribute("transferFilter") AdminTransferFilterForm filter,
            BindingResult bindingResult,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Model model
    ) {
        Page<?> transfers = bindingResult.hasErrors()
                ? emptyPage(pageable)
                : adminService.getTransfers(filter.getKeyword(), filter.getStatus(), pageable);

        model.addAttribute("transfers", transfers);
        model.addAttribute("keyword", filter.getKeyword());
        model.addAttribute("status", filter.getStatus());
        model.addAttribute("transferStatuses", TransferStatus.values());
        model.addAttribute("returnUrl", buildTransfersReturnUrl(filter.getKeyword(), filter.getStatus(), pageable.getPageNumber()));
        return "admin/transfers";
    }

    @PostMapping("/members/{memberId}/deactivate")
    public String deactivateMember(
            @PathVariable Long memberId,
            @RequestParam(required = false) String returnUrl,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        return runAdminAction(
                () -> adminService.deactivateMember(memberId, authentication.getName()),
                "회원 계정을 비활성화했습니다.",
                returnUrl,
                redirectAttributes
        );
    }

    @PostMapping("/members/{memberId}/activate")
    public String activateMember(
            @PathVariable Long memberId,
            @RequestParam(required = false) String returnUrl,
            RedirectAttributes redirectAttributes
    ) {
        return runAdminAction(
                () -> adminService.activateMember(memberId),
                "회원 계정을 다시 활성화했습니다.",
                returnUrl,
                redirectAttributes
        );
    }

    @PostMapping("/members/{memberId}/role")
    public String changeMemberRole(
            @PathVariable Long memberId,
            @RequestParam MemberRole role,
            @RequestParam(required = false) String returnUrl,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        return runAdminAction(
                () -> adminService.changeMemberRole(memberId, role, authentication.getName()),
                "회원 권한을 변경했습니다.",
                returnUrl,
                redirectAttributes
        );
    }

    @PostMapping("/members/{memberId}/delete")
    public String deleteMember(
            @PathVariable Long memberId,
            @RequestParam(required = false) String returnUrl,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        return runAdminAction(
                () -> adminService.deleteMember(memberId, authentication.getName()),
                "회원 데이터를 삭제했습니다.",
                returnUrl,
                redirectAttributes
        );
    }

    @PostMapping("/posts/{postId}/delete")
    public String deletePost(
            @PathVariable Long postId,
            @RequestParam(required = false) String returnUrl,
            RedirectAttributes redirectAttributes
    ) {
        return runAdminAction(
                () -> adminService.deletePost(postId),
                "게시글을 삭제했습니다.",
                returnUrl,
                redirectAttributes
        );
    }

    @PostMapping("/transfers/{transferId}/ticket-sent")
    public String markTransferTicketSent(
            @PathVariable Long transferId,
            @RequestParam(required = false) String returnUrl,
            RedirectAttributes redirectAttributes
    ) {
        return runAdminAction(
                () -> adminService.markTransferTicketSent(transferId),
                "거래를 티켓 전달 완료 상태로 변경했습니다.",
                returnUrl,
                redirectAttributes
        );
    }

    @PostMapping("/transfers/{transferId}/complete")
    public String completeTransfer(
            @PathVariable Long transferId,
            @RequestParam(required = false) String returnUrl,
            RedirectAttributes redirectAttributes
    ) {
        return runAdminAction(
                () -> adminService.completeTransfer(transferId),
                "거래를 완료 처리했습니다.",
                returnUrl,
                redirectAttributes
        );
    }

    @PostMapping("/transfers/{transferId}/cancel")
    public String cancelTransfer(
            @PathVariable Long transferId,
            @RequestParam(required = false) String returnUrl,
            RedirectAttributes redirectAttributes
    ) {
        return runAdminAction(
                () -> adminService.cancelTransfer(transferId),
                "거래를 취소 처리했습니다.",
                returnUrl,
                redirectAttributes
        );
    }

    private Page<?> emptyPage(Pageable pageable) {
        return new PageImpl<>(List.of(), pageable, 0);
    }

    private String runAdminAction(Runnable action, String successMessage, String returnUrl, RedirectAttributes redirectAttributes) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("message", successMessage);
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirectTo(returnUrl);
    }

    private String redirectTo(String returnUrl) {
        if (returnUrl != null && returnUrl.startsWith("/admin")) {
            return "redirect:" + returnUrl;
        }
        return "redirect:/admin/dashboard";
    }

    private String buildMembersReturnUrl(String keyword, Boolean active, MemberRole role, int page) {
        StringBuilder builder = new StringBuilder("/admin/members?page=").append(page);
        append(builder, "keyword", keyword);
        append(builder, "active", active);
        append(builder, "role", role);
        return builder.toString();
    }

    private String buildPostsReturnUrl(String keyword, BoardType boardType, PostStatus status, int page) {
        StringBuilder builder = new StringBuilder("/admin/posts?page=").append(page);
        append(builder, "keyword", keyword);
        append(builder, "boardType", boardType);
        append(builder, "status", status);
        return builder.toString();
    }

    private String buildTransfersReturnUrl(String keyword, TransferStatus status, int page) {
        StringBuilder builder = new StringBuilder("/admin/transfers?page=").append(page);
        append(builder, "keyword", keyword);
        append(builder, "status", status);
        return builder.toString();
    }

    private void append(StringBuilder builder, String key, Object value) {
        if (value != null) {
            builder.append("&")
                    .append(key)
                    .append("=")
                    .append(UriUtils.encodeQueryParam(String.valueOf(value), StandardCharsets.UTF_8));
        }
    }
}
