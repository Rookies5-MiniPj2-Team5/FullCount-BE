package com.fullcount.service;

import com.fullcount.domain.Member;
import com.fullcount.domain.Team;
import com.fullcount.dto.AuthDto;
import com.fullcount.exception.BusinessException;
import com.fullcount.exception.ErrorCode;
import com.fullcount.repository.MemberRepository;
import com.fullcount.repository.TeamRepository;
import com.fullcount.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public void signup(AuthDto.SignupRequest req) {
        if (memberRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (memberRepository.existsByNickname(req.getNickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        Team team = teamRepository.findById(req.getTeamId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        Member member = Member.builder()
                .email(req.getEmail())
                .nickname(req.getNickname())
                .password(passwordEncoder.encode(req.getPassword()))
                .team(team)
                .build();

        memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public AuthDto.TokenResponse login(AuthDto.LoginRequest req) {
        Member member = memberRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(req.getPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getEmail(), member.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(member.getId());

        return new AuthDto.TokenResponse(accessToken, refreshToken);
    }
}
