package com.fullcount.controller;

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
import com.fullcount.service.AdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminController adminController;

    @MockBean
    private AdminService adminService;

    @Test
    @DisplayName("대시보드 페이지는 요약 정보를 모델에 담아 렌더링한다")
    void dashboard_returnsDashboardViewWithSummary() throws Exception {
        DashboardSummary summary = DashboardSummary.builder()
                .memberCount(10)
                .activeMemberCount(8)
                .inactiveMemberCount(2)
                .adminCount(1)
                .postCount(7)
                .openPostCount(4)
                .reservedPostCount(2)
                .closedPostCount(1)
                .transferCount(5)
                .pendingTransferCount(3)
                .completedTransferCount(1)
                .cancelledTransferCount(1)
                .build();
        List<Member> recentMembers = List.of(Member.builder().id(1L).email("member@test.com").nickname("member").password("pw").build());
        List<Post> recentPosts = List.of(Post.builder()
                .id(2L)
                .author(Member.builder().id(10L).email("writer@test.com").nickname("writer").password("pw").build())
                .boardType(BoardType.TRANSFER)
                .title("latest post")
                .content("content")
                .status(PostStatus.OPEN)
                .build());
        List<Transfer> recentTransfers = List.of(Transfer.builder()
                .id(3L)
                .post(Post.builder()
                        .id(4L)
                        .author(Member.builder().id(11L).email("seller@test.com").nickname("seller").password("pw").build())
                        .boardType(BoardType.TRANSFER)
                        .title("latest transfer")
                        .content("content")
                        .status(PostStatus.OPEN)
                        .build())
                .seller(Member.builder().id(11L).email("seller@test.com").nickname("seller").password("pw").build())
                .price(1000)
                .status(TransferStatus.REQUESTED)
                .build());
        when(adminService.getDashboardSummary()).thenReturn(summary);
        when(adminService.getRecentMembersForDashboard()).thenReturn(recentMembers);
        when(adminService.getRecentPostsForDashboard()).thenReturn(recentPosts);
        when(adminService.getRecentTransfersForDashboard()).thenReturn(recentTransfers);

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attribute("summary", summary))
                .andExpect(model().attribute("recentMembers", recentMembers))
                .andExpect(model().attribute("recentPosts", recentPosts))
                .andExpect(model().attribute("recentTransfers", recentTransfers));
    }

    @Test
    @DisplayName("회원 관리 페이지는 필터 값과 인코딩된 returnUrl을 모델에 담는다")
    void members_returnsViewWithFiltersAndEncodedReturnUrl() throws Exception {
        PageImpl<Member> page = new PageImpl<>(List.of(), PageRequest.of(2, 20), 0);
        when(adminService.getMembers(org.mockito.ArgumentMatchers.eq("A B"), org.mockito.ArgumentMatchers.eq(true),
                org.mockito.ArgumentMatchers.eq(MemberRole.ADMIN), any())).thenReturn(page);

        mockMvc.perform(get("/admin/members")
                        .param("keyword", "A B")
                        .param("active", "true")
                        .param("role", "ADMIN")
                        .param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/members"))
                .andExpect(model().attribute("keyword", "A B"))
                .andExpect(model().attribute("active", true))
                .andExpect(model().attribute("role", MemberRole.ADMIN))
                .andExpect(model().attributeExists("roles"))
                .andExpect(model().attribute("returnUrl", "/admin/members?page=2&keyword=A%20B&active=true&role=ADMIN"));
    }

    @Test
    @DisplayName("게시글 관리 페이지는 게시판/상태 필터와 returnUrl을 모델에 담는다")
    void posts_returnsViewWithFiltersAndReturnUrl() throws Exception {
        PageImpl<Post> page = new PageImpl<>(List.of(), PageRequest.of(1, 20), 0);
        when(adminService.getPosts(org.mockito.ArgumentMatchers.eq("ticket"), org.mockito.ArgumentMatchers.eq(BoardType.TRANSFER),
                org.mockito.ArgumentMatchers.eq(PostStatus.OPEN), any())).thenReturn(page);

        mockMvc.perform(get("/admin/posts")
                        .param("keyword", "ticket")
                        .param("boardType", "TRANSFER")
                        .param("status", "OPEN")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/posts"))
                .andExpect(model().attribute("boardType", BoardType.TRANSFER))
                .andExpect(model().attribute("status", PostStatus.OPEN))
                .andExpect(model().attributeExists("boardTypes"))
                .andExpect(model().attributeExists("postStatuses"))
                .andExpect(model().attribute("returnUrl", "/admin/posts?page=1&keyword=ticket&boardType=TRANSFER&status=OPEN"));
    }

    @Test
    @DisplayName("양도 관리 페이지는 상태 필터와 returnUrl을 모델에 담는다")
    void transfers_returnsViewWithFiltersAndReturnUrl() throws Exception {
        PageImpl<Transfer> page = new PageImpl<>(List.of(), PageRequest.of(3, 20), 0);
        when(adminService.getTransfers(org.mockito.ArgumentMatchers.eq("seller"), org.mockito.ArgumentMatchers.eq(TransferStatus.TICKET_SENT),
                any())).thenReturn(page);

        mockMvc.perform(get("/admin/transfers")
                        .param("keyword", "seller")
                        .param("status", "TICKET_SENT")
                        .param("page", "3"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/transfers"))
                .andExpect(model().attribute("status", TransferStatus.TICKET_SENT))
                .andExpect(model().attributeExists("transferStatuses"))
                .andExpect(model().attribute("returnUrl", "/admin/transfers?page=3&keyword=seller&status=TICKET_SENT"));
    }

    @Test
    @DisplayName("회원 비활성화 성공 시 원래 목록으로 리다이렉트하고 성공 메시지를 남긴다")
    void deactivateMember_success_redirectsToReturnUrlAndSetsMessage() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        Authentication authentication = authentication("admin@test.com");

        String view = adminController.deactivateMember(7L, "/admin/members?page=1", authentication, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/members?page=1");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("message");
        verify(adminService).deactivateMember(7L, "admin@test.com");
    }

    @Test
    @DisplayName("회원 비활성화 실패 시 에러 메시지를 플래시에 담고 대시보드로 보낸다")
    void deactivateMember_whenBusinessException_setsErrorFlash() {
        doThrow(new BusinessException(ErrorCode.ACCESS_DENIED, "blocked"))
                .when(adminService).deactivateMember(7L, "admin@test.com");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        Authentication authentication = authentication("admin@test.com");

        String view = adminController.deactivateMember(7L, null, authentication, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/dashboard");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo("blocked");
    }

    @Test
    @DisplayName("권한 변경 요청은 로그인한 관리자 이메일을 서비스에 전달한다")
    void changeMemberRole_passesAuthenticatedAdminEmail() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        Authentication authentication = authentication("root@test.com");

        String view = adminController.changeMemberRole(9L, MemberRole.ADMIN, "/admin/members?page=0", authentication, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/members?page=0");
        verify(adminService).changeMemberRole(9L, MemberRole.ADMIN, "root@test.com");
    }

    @Test
    @DisplayName("안전하지 않은 returnUrl이 들어오면 관리자 대시보드로 리다이렉트한다")
    void deletePost_withUnsafeReturnUrl_redirectsDashboard() throws Exception {
        mockMvc.perform(post("/admin/posts/5/delete")
                        .param("returnUrl", "https://evil.test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));

        verify(adminService).deletePost(5L);
    }

    private Authentication authentication(String email) {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(email, "pw");
    }
}
