package com.fullcount.service;

import com.fullcount.domain.Member;
import com.fullcount.domain.Team;
import com.fullcount.dto.MemberDto;
import com.fullcount.exception.BusinessException;
import com.fullcount.exception.ErrorCode;
import com.fullcount.mapper.MemberMapper; // 매퍼 임포트
import com.fullcount.repository.MemberRepository;
import com.fullcount.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;

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

        Team newTeam = teamRepository.findById(req.getTeamId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        member.changeTeam(newTeam);
    }
}