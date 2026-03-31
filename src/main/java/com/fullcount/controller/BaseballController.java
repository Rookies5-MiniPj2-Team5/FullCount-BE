package com.fullcount.controller;

import com.fullcount.dto.BaseballGameDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fullcount.service.BaseballService;

import java.util.List;

@RestController
@RequestMapping("/api/baseball")
@RequiredArgsConstructor
public class BaseballController {

    private final BaseballService baseballService;

    // 1. 유저 조회용: DB에 있는 올해 일정 전체 가져오기
    // 예: GET /api/baseball/season?year=2026
    @GetMapping("/season")
    public ResponseEntity<List<BaseballGameDto>> getSeasonSchedule(
            @RequestParam(defaultValue = "2026") String year) {
        return ResponseEntity.ok(baseballService.getSeasonSchedule(year));
    }

    // 2. 관리자용: 네이버 API 긁어서 DB에 최신화하기 (최초 1회 필수 실행)
    // 예: POST /api/baseball/sync?year=2026
    @PostMapping("/sync")
    public ResponseEntity<String> syncSchedule(@RequestParam(defaultValue = "2026") int year) {
        baseballService.syncSeasonSchedule(year);
        return ResponseEntity.ok(year + "년도 데이터 DB 동기화가 성공적으로 완료되었습니다.");
    }
}