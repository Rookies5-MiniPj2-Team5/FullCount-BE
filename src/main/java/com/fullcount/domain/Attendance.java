package com.fullcount.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Attendance {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member; // 작성자

    private LocalDate matchDate; // 직관 날짜

    @Enumerated(EnumType.STRING)
    private MatchResult result; // 승패 결과

    private String imageUrl; // 로컬에 저장된 이미지 접근 주소 (예: /uploads/images/xxx.jpg)
}