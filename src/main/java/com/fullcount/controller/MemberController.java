package com.fullcount.controller;

import com.fullcount.dto.MemberDto;
import com.fullcount.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Member", description = "회원 API")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public ResponseEntity<MemberDto.MemberResponse> getMyInfo(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(memberService.getMyInfo(memberId));
    }

    @Operation(summary = "닉네임 수정")
    @PutMapping("/me")
    public ResponseEntity<MemberDto.UpdateNickNameResponse> updateNickname(
            @AuthenticationPrincipal Long memberId,
            @RequestBody @Valid MemberDto.UpdateNickNameRequest req) {
        return ResponseEntity.ok(memberService.updateNickname(memberId, req));
    }

    @Operation(summary = "응원 팀 변경 (시즌 1회)")
    @PutMapping("/me/team")
    public ResponseEntity<Void> changeTeam(
            @AuthenticationPrincipal Long memberId,
            @RequestBody @Valid MemberDto.ChangeTeamRequest req) {
        memberService.changeTeam(memberId, req);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "프로필 이미지 변경")
    @PutMapping("/me/profile-image")
    public ResponseEntity<Void> updateProfileImage(
            @AuthenticationPrincipal Long memberId,
            @RequestBody @Valid MemberDto.UpdateProfileImageRequest req) {
        memberService.updateProfileImage(memberId, req);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "비밀번호 변경")
    @PutMapping("/me/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal Long memberId,
            @RequestBody @Valid MemberDto.UpdatePasswordRequest req) {
        memberService.updatePassword(memberId, req);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "알림 설정 변경")
    @PutMapping("/me/alerts")
    public ResponseEntity<Void> updateAlerts(
            @AuthenticationPrincipal Long memberId,
            @RequestBody @Valid MemberDto.UpdateAlertRequest req) {
        memberService.updateAlerts(memberId, req);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "잔액 충전")
    @PutMapping("/me/charge")
    public ResponseEntity<Void> updateBalance(
            @AuthenticationPrincipal Long memberId,
            @RequestBody @Valid MemberDto.UpdateBalanceRequest req){
        memberService.updateBalance(memberId, req);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "내가 참여 중인 크루 목록 조회")
    @GetMapping("/me/crews")
    public ResponseEntity<java.util.List<MemberDto.MyActivityResponse>> getMyCrews(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(memberService.getMyCrews(memberId));
    }

    @Operation(summary = "내가 작성한 모집글 목록 조회")
    @GetMapping("/me/posts")
    public ResponseEntity<java.util.List<MemberDto.MyActivityResponse>> getMyPosts(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(memberService.getMyPosts(memberId));
    }

    @Operation(summary = "나의 티켓 양도/양수 내역 조회")
    @GetMapping("/me/transfers")
    public ResponseEntity<java.util.List<MemberDto.MyActivityResponse>> getMyTransfers(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(memberService.getMyTransfers(memberId));
    }
}
