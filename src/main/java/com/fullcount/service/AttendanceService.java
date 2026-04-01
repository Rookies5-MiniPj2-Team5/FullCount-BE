package com.fullcount.service;

import com.fullcount.domain.Attendance;
import com.fullcount.domain.MatchResult;
import com.fullcount.domain.Member;
import com.fullcount.dto.AttendanceDto;
import com.fullcount.repository.AttendanceRepository;
import com.fullcount.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final MemberRepository memberRepository;

    // 이미지가 저장될 실제 컴퓨터 폴더 경로 (폴더를 미리 만들어두세요!)
    private final String UPLOAD_DIR = "C:/fullcount/uploads/";

    @Transactional
    public AttendanceDto.Response saveAttendance(Long memberId, AttendanceDto.CreateRequest request) throws IOException {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        String savedImageUrl = null;
        MultipartFile imageFile = request.getImage();

        // 1. 이미지 파일이 있으면 로컬 폴더에 저장
        if (imageFile != null && !imageFile.isEmpty()) {
            // 파일명 중복 방지를 위한 UUID 생성 (예: 1234-abcd_사진.jpg)
            String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
            File dest = new File(UPLOAD_DIR + fileName);

            // 폴더가 없으면 생성
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }
            // 물리적 폴더에 파일 저장
            imageFile.transferTo(dest);

            // DB에 저장할 접근 주소
            savedImageUrl = "/uploads/" + fileName;
        }

        // 2. DB에 기록 저장
        Attendance attendance = Attendance.builder()
                .member(member)
                .matchDate(request.getDate())
                .result(request.getResult())
                .imageUrl(savedImageUrl)
                .build();

        Attendance savedAttendance = attendanceRepository.save(attendance);

        return AttendanceDto.Response.builder()
                .id(savedAttendance.getId())
                .date(savedAttendance.getMatchDate())
                .result(savedAttendance.getResult())
                .imageUrl(savedAttendance.getImageUrl())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AttendanceDto.Response> getMyAttendances(Long memberId) {
        return attendanceRepository.findAllByMemberIdOrderByMatchDateDesc(memberId)
                .stream()
                .map(a -> AttendanceDto.Response.builder()
                        .id(a.getId())
                        .date(a.getMatchDate())
                        .result(a.getResult())
                        .imageUrl(a.getImageUrl())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAttendance(Long attendanceId, Long memberId) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("기록을 찾을 수 없습니다."));

        // 권한 체크 로직 생략 (본인 글인지 확인 필요)

        // 로컬에 저장된 실제 이미지 파일도 함께 삭제
        if (attendance.getImageUrl() != null) {
            String fileName = attendance.getImageUrl().replace("/uploads/", "");
            File file = new File(UPLOAD_DIR + fileName);
            if (file.exists()) {
                file.delete();
            }
        }

        attendanceRepository.delete(attendance);
    }
}