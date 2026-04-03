package com.fullcount.mapper;

import com.fullcount.domain.Member;
import com.fullcount.domain.MemberRole;
import com.fullcount.domain.Team;
import com.fullcount.dto.AuthDto;
import com.fullcount.dto.MemberDto;

public class MemberMapper {
    // Entity -> Response DTO 변환
    public static MemberDto.MemberResponse toMemberResponse(Member member) {
        if (member == null) return null;

        return MemberDto.MemberResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .teamName(member.getTeam() != null ? member.getTeam().getName() : null)
                .teamShortName(member.getTeam() != null ? member.getTeam().getShortName() : null)
                .badgeLevel(member.getBadgeLevel().name())
                .mannerTemperature(member.getMannerTemperature())
                .role(member.getRole().name())
                .profileImageUrl(member.getProfileImageUrl()) // 프로필 이미지 URL
                .chatAlert(member.getChatAlert())             // ⭐️ 알림 설정: 채팅
                .transferAlert(member.getTransferAlert())     // ⭐️ 알림 설정: 양도 거래
                .mannerAlert(member.getMannerAlert())         // ⭐️ 알림 설정: 매너 평가
                .balance(member.getBalance())
                .build();
    }

    public static MemberDto.UpdateNickNameResponse toUpdateNickNameResponse(String nickName){
        return MemberDto.UpdateNickNameResponse.builder()
                .nickname(nickName)
                .build();
    }

    // 회원가입 시 Member 생성
    public static Member toMember(AuthDto.SignupRequest req, Team team, String encodedPassword) {
        return Member.builder()
                .email(req.getEmail())
                .nickname(req.getNickname())
                .password(encodedPassword)
                .team(team)
                .role(MemberRole.USER)
                .build();
    }
}