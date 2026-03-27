package com.fullcount.controller;

import com.fullcount.domain.Team;
import com.fullcount.exception.BusinessException;
import com.fullcount.exception.ErrorCode;
import com.fullcount.repository.TeamRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Team", description = "KBO 구단 API")
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamRepository teamRepository;

    @Operation(summary = "전체 구단 목록 조회")
    @GetMapping
    public ResponseEntity<List<Team>> getTeams() {
        return ResponseEntity.ok(teamRepository.findAll());
    }

    @Operation(summary = "구단 단건 조회")
    @GetMapping("/{id}")
    public ResponseEntity<Team> getTeam(@PathVariable Long id) {
        return ResponseEntity.ok(teamRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND)));
    }
}
