package com.fullcount.service;

import com.fullcount.domain.Attendance;
import com.fullcount.domain.MatchResult;
import com.fullcount.domain.Member;
import com.fullcount.dto.AttendanceDto;
import com.fullcount.repository.AttendanceRepository;
import com.fullcount.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Transactional
    public AttendanceDto.Response saveAttendance(Long memberId, AttendanceDto.CreateRequest request) throws IOException {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        String savedImageUrl = null;
        MultipartFile imageFile = request.getImage();

        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
            File dest = new File(uploadDir + fileName);

            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }
            imageFile.transferTo(dest);

            savedImageUrl = "/uploads/" + fileName;
        }

        //엔티티 생성 시 memo 데이터 포함
        Attendance attendance = Attendance.builder()
                .member(member)
                .matchDate(request.getDate())
                .result(request.getResult())
                .imageUrl(savedImageUrl)
                .memo(request.getMemo()) //메모 저장 로직 추가
                .build();

        Attendance savedAttendance = attendanceRepository.save(attendance);

        //수정된 부분: 응답 DTO 생성 시 memo 데이터 포함
        return AttendanceDto.Response.builder()
                .id(savedAttendance.getId())
                .date(savedAttendance.getMatchDate())
                .result(savedAttendance.getResult())
                .imageUrl(savedAttendance.getImageUrl())
                .memo(savedAttendance.getMemo()) //메모 반환 로직 추가
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
                        .memo(a.getMemo()) //목록 조회 시 메모 반환 로직 추가
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAttendance(Long attendanceId, Long memberId) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("기록을 찾을 수 없습니다."));

        if (attendance.getImageUrl() != null) {
            String fileName = attendance.getImageUrl().replace("/uploads/", "");
            File file = new File(uploadDir + fileName);
            if (file.exists()) {
                file.delete();
            }
        }

        attendanceRepository.delete(attendance);
    }
}