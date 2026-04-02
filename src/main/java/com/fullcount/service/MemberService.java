package com.fullcount.service;

import com.fullcount.domain.Member;
import com.fullcount.domain.Team;
import com.fullcount.dto.MemberDto;
import com.fullcount.exception.BusinessException;
import com.fullcount.exception.ErrorCode;
import com.fullcount.mapper.MemberMapper;
import com.fullcount.repository.MemberRepository;
import com.fullcount.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;

    // ⭐️ 중요: final 변수는 반드시 생성자나 클래스 최상단에 모여 있어야 주입(DI)이 정상적으로 됩니다.
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public MemberDto.MemberResponse getMyInfo(Long memberId) {
        Member member = memberRepository.findByIdWithTeam(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return MemberMapper.toMemberResponse(member);
    }

    @Transactional
    public MemberDto.UpdateNickNameResponse updateNickname(Long memberId, MemberDto.UpdateNickNameRequest req) {
        // N+1 예방을 위해 findByIdWithTeam 사용 (DTO 변환 시 Team 정보 필요하므로)
        Member member = memberRepository.findByIdWithTeam(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (req.getNickname() != null && !req.getNickname().equals(member.getNickname())) {
            if (memberRepository.existsByNickname(req.getNickname())) {
                throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
            }
            member.updateNickname(req.getNickname());
        }

        return MemberMapper.toUpdateNickNameResponse(member.getNickname());
    }

    @Transactional
    public void changeTeam(Long memberId, MemberDto.ChangeTeamRequest req) {
        Member member = memberRepository.findByIdWithTeam(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Team newTeam = findTeam(req.getTeamId());

        member.changeTeam(newTeam);
    }

    private Team findTeam(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
        }

        try {
            Long id = Long.parseLong(identifier);
            return teamRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        } catch (NumberFormatException e) {
            return teamRepository.findByShortName(identifier)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        }
    }

    @Transactional
    public void updateProfileImage(Long memberId, MemberDto.UpdateProfileImageRequest req) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        member.updateProfileImageUrl(req.getProfileImageUrl());
    }

    @Transactional
    public void updatePassword(Long memberId, MemberDto.UpdatePasswordRequest req) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 1. 현재 비밀번호가 맞는지 확인 (500 에러 방지를 위해 BusinessException 사용)
        if (!passwordEncoder.matches(req.getCurrentPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // 2. 새 비밀번호 암호화 후 변경
        member.updatePassword(passwordEncoder.encode(req.getNewPassword()));
    }

    @Transactional
    public void updateAlerts(Long memberId, MemberDto.UpdateAlertRequest req) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        member.updateAlerts(req.getChatAlert(), req.getTransferAlert(), req.getMannerAlert());
    }
}