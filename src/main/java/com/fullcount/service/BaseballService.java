package com.fullcount.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullcount.domain.BaseballGame;
import com.fullcount.dto.BaseballGameDto;
import com.fullcount.repository.BaseballGameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseballService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BaseballGameRepository baseballGameRepository; // DB 연동용 리포지토리 주입

    // 1. 일반 유저용: 네이버 API를 찌르지 않고 우리 DB에서만 빠르게 꺼내서 전달
    @Transactional(readOnly = true)
    public List<BaseballGameDto> getSeasonSchedule(String year) {
        List<BaseballGame> games = baseballGameRepository.findByGameDateStartingWithOrderByGameDateAscGameTimeAsc(year);

        return games.stream().map(game -> BaseballGameDto.builder()
                .gameId(game.getGameId())
                .isCanceled(game.isCanceled())
                .gameDate(game.getGameDate())
                .gameTime(game.getGameTime())
                .homeTeam(game.getHomeTeam())
                .awayTeam(game.getAwayTeam())
                .homeScore(game.getHomeScore())
                .awayScore(game.getAwayScore())
                .stadium(game.getStadium())
                .status(game.getStatus())
                .build()).collect(Collectors.toList());
    }

    // 2. 관리자용: 네이버 API에서 데이터를 긁어와서 우리 DB에 동기화(Upsert)
    @Transactional
    public void syncSeasonSchedule(int year) {
        for (int month = 3; month <= 10; month++) {
            String dateParam = String.format("%d-%02d-01", year, month);
            List<BaseballGameDto> monthlyData = fetchMonthlyFromApi(dateParam);

            for (BaseballGameDto dto : monthlyData) {
                Optional<BaseballGame> existingGame = baseballGameRepository.findByGameId(dto.getGameId());

                if (existingGame.isPresent()) {
                    // 이미 DB에 있는 경기면 점수, 취소여부, 상태만 최신으로 업데이트
                    BaseballGame game = existingGame.get();
                    game.setCanceled(dto.isCanceled());
                    game.setHomeScore(dto.getHomeScore());
                    game.setAwayScore(dto.getAwayScore());
                    game.setStatus(dto.getStatus());
                    baseballGameRepository.save(game);
                } else {
                    // DB에 없는 새로운 경기면 신규 저장
                    BaseballGame newGame = BaseballGame.builder()
                            .gameId(dto.getGameId())
                            .isCanceled(dto.isCanceled())
                            .gameDate(dto.getGameDate())
                            .gameTime(dto.getGameTime())
                            .homeTeam(dto.getHomeTeam())
                            .awayTeam(dto.getAwayTeam())
                            .homeScore(dto.getHomeScore())
                            .awayScore(dto.getAwayScore())
                            .stadium(dto.getStadium())
                            .status(dto.getStatus())
                            .build();
                    baseballGameRepository.save(newGame);
                }
            }
            try {
                Thread.sleep(500); // 0.5초 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("{}년도 야구 일정 DB 동기화 완료!", year);
    }

    // 네이버 API 실제 호출 로직 (내부에서만 사용)
    private List<BaseballGameDto> fetchMonthlyFromApi(String date) {
        String url = UriComponentsBuilder.fromHttpUrl("https://api-gw.sports.naver.com/schedule/calendar")
                .queryParam("upperCategoryId", "kbaseball")
                .queryParam("categoryIds", ",kbo,kbs,kbaseballetc,premier12,apbc")
                .queryParam("date", date)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        List<BaseballGameDto> gameList = new ArrayList<>();

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode datesNode = rootNode.path("result").path("dates");

            if (datesNode.isArray()) {
                for (JsonNode dateNode : datesNode) {
                    String ymd = dateNode.path("ymd").asText();
                    JsonNode gameInfos = dateNode.path("gameInfos");
                    
                    if (gameInfos.isArray()) {
                        for (JsonNode game : gameInfos) {
                            String homeCode = game.path("homeTeamCode").asText();
                            String awayCode = game.path("awayTeamCode").asText();
                            // 올스타전 등 빈 팀코드가 있는 경우 제외
                            if(homeCode.isEmpty() || awayCode.isEmpty()) continue;
                            
                            gameList.add(BaseballGameDto.builder()
                                    .gameId(game.path("gameId").asText())
                                    .isCanceled("CANCEL".equals(game.path("statusCode").asText()))
                                    .gameDate(ymd)
                                    .gameTime("") // calendar 조회시 시간 미제공
                                    .homeTeam(homeCode)
                                    .awayTeam(awayCode)
                                    .homeScore(0) // calendar 조회시 점수 미제공
                                    .awayScore(0)
                                    .stadium("") // calendar 조회시 구장 미제공
                                    .status(game.path("statusCode").asText())
                                    .build());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("API 연동 중 오류 발생: {}", e.getMessage());
        }
        return gameList;
    }
}