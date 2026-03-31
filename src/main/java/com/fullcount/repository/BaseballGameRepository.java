package com.fullcount.repository;

import com.fullcount.domain.BaseballGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BaseballGameRepository extends JpaRepository<BaseballGame, Long> {
    
    // 특정 게임 ID가 이미 DB에 있는지 확인하는 용도
    Optional<BaseballGame> findByGameId(String gameId);

    // "2026"으로 시작하는 날짜의 데이터를 날짜/시간순으로 가져오는 용도
    List<BaseballGame> findByGameDateStartingWithOrderByGameDateAscGameTimeAsc(String year);
}