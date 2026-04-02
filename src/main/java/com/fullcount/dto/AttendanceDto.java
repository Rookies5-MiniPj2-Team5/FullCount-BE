package com.fullcount.dto;

import com.fullcount.domain.MatchResult;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class AttendanceDto {

    // 프론트에서 넘어오는 데이터 (FormData)
    @Data
    public static class CreateRequest {
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;

        private MatchResult result;
        private MultipartFile image;
    }

    // 프론트로 보내주는 데이터
    @Data
    @Builder
    public static class Response {
        private Long id;
        private LocalDate date;
        private MatchResult result;
        private String imageUrl;
    }
}