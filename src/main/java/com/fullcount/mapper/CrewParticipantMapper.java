package com.fullcount.mapper;

import com.fullcount.domain.CrewParticipant;
import com.fullcount.domain.Member;
import com.fullcount.domain.Post;
import com.fullcount.dto.PostDto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CrewParticipantMapper {

    /** Member -> CrewParticipant 엔티티 변환 */
    public static CrewParticipant toEntity(Post post, Member member, boolean isLeader, String applyMessage, boolean isApproved) {
        return CrewParticipant.builder()
                .post(post)
                .member(member)
                .isLeader(isLeader)
                .applyMessage(applyMessage)
                .isApproved(isApproved)
                .build();
    }

    /** CrewParticipant 엔티티 -> CrewMemberResponse DTO 변환 */
    public static PostDto.CrewMemberResponse toResponse(CrewParticipant participant) {
        if (participant == null) return null;

        return PostDto.CrewMemberResponse.builder()
                .nickname(participant.getMember().getNickname())
                .mannerTemperature(participant.getMember().getMannerTemperature())
                .isLeader(participant.getIsLeader())
                .profileImage(participant.getMember().getProfileImageUrl())
                .applyMessage(participant.getApplyMessage())
                .isApproved(participant.getIsApproved())
                .build();
    }
}
