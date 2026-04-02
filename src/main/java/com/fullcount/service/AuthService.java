package com.fullcount.service;

import com.fullcount.domain.Member;
import com.fullcount.domain.MemberRole;
import com.fullcount.domain.RefreshToken;
import com.fullcount.domain.Team;
import com.fullcount.dto.AuthDto;
import com.fullcount.exception.BusinessException;
import com.fullcount.exception.ErrorCode;
import com.fullcount.mapper.MemberMapper;
import com.fullcount.mapper.RefreshTokenMapper;
import com.fullcount.repository.MemberRepository;
import com.fullcount.repository.RefreshTokenRepository;
import com.fullcount.repository.TeamRepository;
import com.fullcount.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /**
     * 회원가입
     * - 이메일 중복 여부 확인
     * - 닉네임 중복 여부 확인
     * - 선택한 응원 팀 존재 여부 확인
     * - 비밀번호 암호화 후 회원 저장
     */
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

        String encodedPassword = passwordEncoder.encode(req.getPassword());

        Member savedMember = memberRepository.save(MemberMapper.toMember(req, team, encodedPassword));

        log.info("회원가입 완료 - memberId: {}, email: {}, teamId: {}", savedMember.getId(), savedMember.getEmail(), team.getId());
    }

    /**
     * 로그인
     * - 이메일로 회원 조회
     * - 계정 활성화 여부 확인
     * - 비밀번호 검증
     * - Access Token / Refresh Token 발급
     */
    @Transactional
    public AuthDto.TokenResponse login(AuthDto.LoginRequest req) {
        Member member = memberRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 비활성화된 회원인지 확인
        if (!Boolean.TRUE.equals(member.getIsActive())) {
            throw new BusinessException(ErrorCode.INACTIVE_MEMBER);
        }

        // 입력한 비밀번호와 저장된 암호화 비밀번호 비교
        if (!passwordEncoder.matches(req.getPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        log.info("로그인 성공 - memberId: {}, email: {}", member.getId(), member.getEmail());

        return generateTokenSet(member.getId(), member.getEmail(), member.getRole());
    }

    /**
     * Access Token / Refresh Token 재발급
     * - 전달받은 Refresh Token이 DB에 존재하는지 확인
     * - Refresh Token 만료 여부 확인
     * - 해당 회원이 존재하는지 확인
     * - 새로운 토큰 세트 재발급
     */
    @Transactional
    public AuthDto.TokenResponse refresh(AuthDto.RefreshTokenRequest req) {
        String token = req.getRefreshToken();

        // DB에 저장된 Refresh Token인지 확인
        RefreshToken savedToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        // DB 기준 만료 시간 확인
        if (savedToken.getExpiryAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        }

        // 토큰의 소유 회원 조회
        Member member = memberRepository.findById(savedToken.getMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        log.info("토큰 재발급 요청 성공 - memberId: {}, email: {}", member.getId(), member.getEmail());

        // 새로운 Access/Refresh Token 발급
        return generateTokenSet(member.getId(), member.getEmail(), member.getRole());
    }

    /**
     * 공통 토큰 발급 및 저장 로직
     * - Access Token 생성
     * - Refresh Token 생성
     * - Refresh Token 만료 시간 계산
     * - 회원 기준으로 Refresh Token Upsert 처리
     */
    private AuthDto.TokenResponse generateTokenSet(Long memberId, String email, MemberRole role) {
        // Access Token 생성
        String accessToken = jwtProvider.generateAccessToken(memberId, email, role);

        // Refresh Token 생성
        String refreshToken = jwtProvider.generateRefreshToken(memberId);

        // Refresh Token 만료 시각 계산
        LocalDateTime expiryAt = LocalDateTime.now()
                .plusSeconds(jwtProvider.getRefreshTokenExpiry() / 1000);

        // 회원 기준으로 기존 Refresh Token이 있으면 갱신, 없으면 새로 저장
        refreshTokenRepository.findByMemberId(memberId)
                .ifPresentOrElse(
                        saved -> saved.updateToken(refreshToken, expiryAt),
                        () -> refreshTokenRepository.save(
                                RefreshTokenMapper.toRefreshToken(memberId, refreshToken, expiryAt)
                        )
                );

        log.info("토큰 발급 완료 - memberId: {}, email: {}, role: {}", memberId, email, role.name());

        return AuthDto.TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * 로그아웃
     * - 회원 기준으로 저장된 Refresh Token 삭제
     */
    @Transactional
    public void logout(Long memberId) {
        refreshTokenRepository.deleteByMemberId(memberId);
        log.info("로그아웃 완료 - memberId: {}", memberId);
    }
}