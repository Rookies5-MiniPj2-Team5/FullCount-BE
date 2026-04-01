package com.fullcount.controller;

import com.fullcount.dto.AttendanceDto;
import com.fullcount.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Tag(name = "Attendance", description = "직관 달력 기록 API")
@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @Operation(summary = "직관 기록 저장 (사진 포함)")
    // consumes 설정으로 텍스트+파일이 섞인 FormData를 받는다고 명시합니다.
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttendanceDto.Response> createAttendance(
            @AuthenticationPrincipal Long memberId,
            @ModelAttribute AttendanceDto.CreateRequest request) throws IOException { // @RequestBody가 아니라 @ModelAttribute 사용

        return ResponseEntity.ok(attendanceService.saveAttendance(memberId, request));
    }

    @Operation(summary = "내 직관 기록 전체 조회")
    @GetMapping
    public ResponseEntity<List<AttendanceDto.Response>> getMyAttendances(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(attendanceService.getMyAttendances(memberId));
    }

    @Operation(summary = "직관 기록 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttendance(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId) {
        attendanceService.deleteAttendance(id, memberId);
        return ResponseEntity.noContent().build();
    }
}