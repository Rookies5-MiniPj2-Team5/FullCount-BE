package com.fullcount.service;

import com.fullcount.domain.Member;
import com.fullcount.domain.Team;
import com.fullcount.dto.MemberDto;
import com.fullcount.exception.BusinessException;
import com.fullcount.exception.ErrorCode;
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
    public MemberDto.Response getMyInfo(Long memberId) {
        Member member = memberRepository.findByIdWithTeam(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return MemberDto.Response.from(member);
    }

    @Transactional
    public MemberDto.Response updateNickname(Long memberId, MemberDto.UpdateRequest req) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (req.getNickname() != null && !req.getNickname().equals(member.getNickname())) {
            if (memberRepository.existsByNickname(req.getNickname())) {
                throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
            }
            member.updateNickname(req.getNickname());
        }
        return MemberDto.Response.from(member);
    }

    @Transactional
    public void changeTeam(Long memberId, MemberDto.ChangeTeamRequest req) {
        Member member = memberRepository.findByIdWithTeam(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Team newTeam = teamRepository.findById(req.getTeamId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        try {
            member.changeTeam(newTeam);
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.TEAM_CHANGE_LIMIT);
        }
    }
}
