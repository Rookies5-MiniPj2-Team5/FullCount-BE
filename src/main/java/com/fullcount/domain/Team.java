package com.fullcount.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "team")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String name;          // 두산 베어스

    @Column(nullable = false, length = 5)
    private String shortName;     // DU

    @Column(nullable = false, length = 50)
    private String homeStadium;   // 잠실야구장
}
