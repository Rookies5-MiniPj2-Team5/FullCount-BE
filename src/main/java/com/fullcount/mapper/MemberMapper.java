package com.fullcount.mapper;

import com.fullcount.domain.Member;
import com.fullcount.domain.MemberRole;
import com.fullcount.domain.Team;
import com.fullcount.dto.AuthDto;

public class MemberMapper {

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