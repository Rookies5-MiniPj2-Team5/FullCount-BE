package com.fullcount.repository;

import com.fullcount.domain.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findAllByMemberIdOrderByMatchDateDesc(Long memberId);
}