package com.fullcount.controller;

import com.fullcount.dto.MemberDto;
import com.fullcount.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
            @RequestBody MemberDto.UpdateNickNameRequest req) {
        return ResponseEntity.ok(memberService.updateNickname(memberId, req));
    }

    @Operation(summary = "응원 팀 변경 (시즌 1회)")
    @PutMapping("/me/team")
    public ResponseEntity<Void> changeTeam(
            @AuthenticationPrincipal Long memberId,
            @RequestBody MemberDto.ChangeTeamRequest req) {
        memberService.changeTeam(memberId, req);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "프로필 이미지 변경")
    @PutMapping("/me/profile-image")
    public ResponseEntity<Void> updateProfileImage(
            @AuthenticationPrincipal Long memberId,
            @RequestBody MemberDto.UpdateProfileImageRequest req) {
        memberService.updateProfileImage(memberId, req);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "비밀번호 변경")
    @PutMapping("/me/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal Long memberId,
            @RequestBody MemberDto.UpdatePasswordRequest req) {
        memberService.updatePassword(memberId, req);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "알림 설정 변경")
    @PutMapping("/me/alerts")
    public ResponseEntity<Void> updateAlerts(
            @AuthenticationPrincipal Long memberId,
            @RequestBody MemberDto.UpdateAlertRequest req) {
        memberService.updateAlerts(memberId, req);
        return ResponseEntity.ok().build();
    }
}
