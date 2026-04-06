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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseballService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BaseballGameRepository baseballGameRepository;

    // ── 팀 코드 매핑 테이블 ──────────────────────────────────────────────────
    // Naver calendar API 팀코드 → 정규화(canonical) 코드
    private static final Map<String, String> NAVER_CODE_TO_CANONICAL = new HashMap<>();
    static {
        NAVER_CODE_TO_CANONICAL.put("LG",  "LG");  // LG
        NAVER_CODE_TO_CANONICAL.put("OB",  "DU");  // 두산 (구 OB)
        NAVER_CODE_TO_CANONICAL.put("DU",  "DU");  // 두산
        NAVER_CODE_TO_CANONICAL.put("SK",  "SSG"); // SSG (구 SK)
        NAVER_CODE_TO_CANONICAL.put("SSG", "SSG"); // SSG
        NAVER_CODE_TO_CANONICAL.put("HT",  "KIA"); // KIA (구 HT)
        NAVER_CODE_TO_CANONICAL.put("KIA", "KIA"); // KIA
        NAVER_CODE_TO_CANONICAL.put("SS",  "SA");  // 삼성 (구 SS)
        NAVER_CODE_TO_CANONICAL.put("SA",  "SA");  // 삼성
        NAVER_CODE_TO_CANONICAL.put("LT",  "LO");  // 롯데 (구 LT)
        NAVER_CODE_TO_CANONICAL.put("LO",  "LO");  // 롯데
        NAVER_CODE_TO_CANONICAL.put("HH",  "HH");  // 한화
        NAVER_CODE_TO_CANONICAL.put("KT",  "KT");  // KT
        NAVER_CODE_TO_CANONICAL.put("NC",  "NC");  // NC
        NAVER_CODE_TO_CANONICAL.put("WO",  "WO");  // 키움
    }

    // KBO 공식 사이트 한국어 팀명 → Naver calendar API 팀코드
    private static final Map<String, String> KBO_NAME_TO_NAVER_CODE = new HashMap<>();
    static {
        KBO_NAME_TO_NAVER_CODE.put("LG",  "LG");
        KBO_NAME_TO_NAVER_CODE.put("두산", "OB");
        KBO_NAME_TO_NAVER_CODE.put("SSG", "SSG");
        KBO_NAME_TO_NAVER_CODE.put("KIA", "HT");
        KBO_NAME_TO_NAVER_CODE.put("기아", "HT");
        KBO_NAME_TO_NAVER_CODE.put("삼성", "SS");
        KBO_NAME_TO_NAVER_CODE.put("롯데", "LT");
        KBO_NAME_TO_NAVER_CODE.put("한화", "HH");
        KBO_NAME_TO_NAVER_CODE.put("KT",  "KT");
        KBO_NAME_TO_NAVER_CODE.put("kt",  "KT");
        KBO_NAME_TO_NAVER_CODE.put("NC",  "NC");
        KBO_NAME_TO_NAVER_CODE.put("nc",  "NC");
        KBO_NAME_TO_NAVER_CODE.put("키움", "WO");
    }

    // 1. 일반 유저용: 네이버 API를 찌르지 않고 우리 DB에서만 빠르게 꺼내서 전달
    @Transactional(readOnly = true)
    public List<BaseballGameDto> getSeasonSchedule(String year) {
        List<BaseballGame> games = baseballGameRepository.findByGameDateStartingWithOrderByGameDateAscGameTimeAsc(year);

        int y = Integer.parseInt(year);
        java.time.LocalDate regularSeasonStart = java.time.LocalDate.of(y, 3, 23); // 정규시즌 개막일 기준

        return games.stream()
                .filter(game -> {
                    java.time.LocalDate gameDateParsed = java.time.LocalDate.parse(game.getGameDate());
                    // 정규시즌 이전 경기 제한 (미래 경기는 폐막까지 쭉 보여줌)
                    return !gameDateParsed.isBefore(regularSeasonStart);
                })
                .map(game -> BaseballGameDto.builder()
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
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate regularSeasonStart = java.time.LocalDate.of(year, 3, 23); // 시범경기 제외
        
        for (int month = 3; month <= 10; month++) {
            String dateParam = String.format("%d-%02d-01", year, month);
            List<BaseballGameDto> monthlyDataResp = fetchMonthlyFromApi(dateParam);

            // 유효한 정규시즌 내 경기만 필터링 (미래 경기는 폐막전까지 일정 보여주기 위해 저장)
            List<BaseballGameDto> monthlyData = monthlyDataResp.stream()
                    .filter(dto -> !java.time.LocalDate.parse(dto.getGameDate()).isBefore(regularSeasonStart))
                    .collect(Collectors.toList());

            if (monthlyData.isEmpty()) continue;

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
            // ── [KBO 스코어 패치] KBO 공식 사이트 스크래핑으로 점수/구장 보정 ──
            Map<String, List<BaseballGameDto>> dateGrouped = monthlyData.stream()
                    .collect(Collectors.groupingBy(BaseballGameDto::getGameDate));

            for (Map.Entry<String, List<BaseballGameDto>> dateEntry : dateGrouped.entrySet()) {
                String gameDate = dateEntry.getKey();
                List<BaseballGameDto> gamesOnDate = dateEntry.getValue();

                List<Map<String, Object>> scraped = scrapeKboScoresByDate(gameDate);
                if (scraped.isEmpty()) {
                    try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    continue;
                }

                for (BaseballGameDto calGame : gamesOnDate) {
                    String canonicalHome = toCanonicalCode(calGame.getHomeTeam());
                    String canonicalAway = toCanonicalCode(calGame.getAwayTeam());

                    scraped.stream()
                        .filter(s -> {
                            String homeNameRaw = ((String) s.get("homeTeamName")).toUpperCase();
                            String awayNameRaw = ((String) s.get("awayTeamName")).toUpperCase();
                            
                            String sh = toCanonicalCode(KBO_NAME_TO_NAVER_CODE.getOrDefault(homeNameRaw, ""));
                            String sa = toCanonicalCode(KBO_NAME_TO_NAVER_CODE.getOrDefault(awayNameRaw, ""));
                            return sh.equals(canonicalHome) && sa.equals(canonicalAway);
                        })
                        .findFirst()
                        .ifPresent(matched -> baseballGameRepository.findByGameId(calGame.getGameId()).ifPresent(dbGame -> {
                            dbGame.setHomeScore((Integer) matched.get("homeScore"));
                            dbGame.setAwayScore((Integer) matched.get("awayScore"));
                            String st = (String) matched.get("stadium");
                            if (st != null && !st.isEmpty()) dbGame.setStadium(st);
                            baseballGameRepository.save(dbGame);
                        }));
                }
                log.info("{} KBO 스코어 패치 완료: {}경기 스크래핑됨", gameDate, scraped.size());
                try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            // ── [KBO 스코어 패치 끝] ──

            try {
                Thread.sleep(500); // 0.5초 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("{}년도 야구 일정 DB 동기화 완료!", year);
    }

    // 3. 네이버 라이브 게임 일정 검색 (오늘의 경기 실시간 조회용)
    @Transactional(readOnly = true)
    public Map<String, Object> getLiveGames(String date) {
        String url = UriComponentsBuilder.fromHttpUrl("https://api-gw.sports.naver.com/schedule/games")
                .queryParam("categoryIds", "kbo")
                .queryParam("date", date)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Referer", "https://sports.news.naver.com/");
        headers.set("Origin", "https://sports.news.naver.com");
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
        headers.set("Accept", "application/json, text/plain, */*");
        headers.set("Accept-Language", "ko-KR,ko;q=0.9");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, String.class);
            com.fasterxml.jackson.databind.JsonNode json = objectMapper.readTree(response.getBody());
            
            // 정상적인 라이브 데이터가 배열에 담겨있는지 확인
            com.fasterxml.jackson.databind.JsonNode gamesNode = json.path("result").path("games");
            if (gamesNode.isMissingNode() || gamesNode.isEmpty()) {
                // 네이버 라이브 데이터가 빈 배열이면 (과거 날짜이거나 경기가 없는 경우)
                // 방금 KBO에서 긁어와서 저장된 DB 데이터를 조회하여 프론트엔드 형식에 맞춰 응답 구조를 생성
                List<BaseballGame> dbGames = baseballGameRepository.findByGameDateStartingWithOrderByGameDateAscGameTimeAsc(date);
                if (!dbGames.isEmpty()) {
                    List<Map<String, Object>> mappedGames = dbGames.stream().map(g -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("gameId", g.getGameId());
                        map.put("homeTeamCode", g.getHomeTeam());
                        map.put("awayTeamCode", g.getAwayTeam());
                        
                        String statusCode = "SCHEDULED";
                        if (g.isCanceled() || "취소".equals(g.getStatus())) {
                            statusCode = "CANCEL";
                        } else if ("종료".equals(g.getStatus()) || (g.getHomeScore() != null && g.getHomeScore() >= 0 && g.getAwayScore() != null && g.getAwayScore() >= 0)) {
                            // 스코어가 존재하면 스크래핑이 완료된 '종료' 기믹으로 처리
                            statusCode = "RESULT";
                        }
                        
                        map.put("statusCode", statusCode);
                        map.put("homeTeamScore", g.getHomeScore());
                        map.put("awayTeamScore", g.getAwayScore());
                        map.put("statusInfo", g.getStatus() != null ? g.getStatus() : "종료");
                        map.put("gameDateTime", g.getGameDate() + "T" + (g.getGameTime() != null ? g.getGameTime() + ":00" : "18:30:00"));
                        return map;
                    }).collect(Collectors.toList());

                    Map<String, Object> resultNode = new HashMap<>();
                    resultNode.put("games", mappedGames);
                    Map<String, Object> finalResponse = new HashMap<>();
                    finalResponse.put("result", resultNode);
                    
                    return finalResponse;
                }
            }

            return objectMapper.convertValue(json, Map.class);
        } catch (Exception e) {
            log.error("라이브 게임 연동 중 오류 발생: {}", e.getMessage());
            return Map.of("error", "fail");
        }
    }

    // 4. KBO 공식 팀 순위 HTML 크롤링
    public List<Map<String, String>> getKboStandings() {
        String url = "https://www.koreabaseball.com/Record/TeamRank/TeamRankDaily.aspx";
        List<Map<String, String>> standings = new ArrayList<>();
        try {
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "ko-KR,ko;q=0.9")
                    .get();

            String[] tableSelectors = {
                    "#content table tbody tr",
                    "table.tData01 tbody tr",
                    "#tblRecord tbody tr",
                    ".record_list table tbody tr",
                    "table tbody tr"
            };

            org.jsoup.select.Elements rows = new org.jsoup.select.Elements();
            for (String selector : tableSelectors) {
                rows = doc.select(selector);
                if (rows.size() >= 8) break;
            }

            Map<String, String> kboTeamMap = Map.of(
                    "LG", "LG", "두산", "DU", "SSG", "SSG", "KIA", "KIA", "삼성", "SA",
                    "롯데", "LO", "한화", "HH", "KT", "KT", "NC", "NC", "키움", "WO"
            );

            for (org.jsoup.nodes.Element row : rows) {
                org.jsoup.select.Elements cells = row.select("td");
                if (cells.size() < 7) continue;

                String rankText = cells.get(0).text().trim();
                int rank;
                try {
                    rank = Integer.parseInt(rankText);
                } catch (NumberFormatException e) {
                    continue;
                }
                if (rank < 1 || rank > 10) continue;

                String teamName = cells.get(1).text().trim();
                String teamId = kboTeamMap.get(teamName);
                if (teamId == null) continue;

                Map<String, String> teamData = new HashMap<>();
                teamData.put("rank", String.valueOf(rank));
                teamData.put("teamId", teamId);
                teamData.put("teamName", teamName);
                teamData.put("games", cells.get(2).text().trim());
                teamData.put("wins", cells.get(3).text().trim());
                teamData.put("losses", cells.get(4).text().trim());
                teamData.put("draws", cells.get(5).text().trim());
                teamData.put("pct", cells.get(6).text().trim());
                teamData.put("gb", cells.get(7).text().trim());

                standings.add(teamData);
            }
        } catch (Exception e) {
            log.error("KBO 순위 크롤링 중 오류: {}", e.getMessage());
        }
        standings.sort(java.util.Comparator.comparingInt(a -> Integer.parseInt(a.get("rank"))));
        return standings;
    }

    // 네이버 API 실제 호출 로직 (내부에서만 사용) — 원본 복원
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
                            if (homeCode.isEmpty() || awayCode.isEmpty()) continue;

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

    /**
     * [점수 전용] 특정 날짜의 KBO 경기 점수만 /schedule/games API에서 조회합니다.
     * syncSeasonSchedule()에서 calendar sync 후 점수 보정(patch)용으로만 사용합니다.
     * key: gameId, value: {homeScore, awayScore, gameTime, stadium}
     */
    private Map<String, BaseballGameDto> fetchScoresByDate(String date) {
        String url = UriComponentsBuilder.fromHttpUrl("https://api-gw.sports.naver.com/schedule/games")
                .queryParam("categoryIds", "kbo")
                .queryParam("date", date)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Referer", "https://sports.news.naver.com/");
        headers.set("Origin", "https://sports.news.naver.com");
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        headers.set("Accept", "application/json, text/plain, */*");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        Map<String, BaseballGameDto> scoreMap = new HashMap<>();
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode games = rootNode.path("result").path("games");

            if (games.isArray()) {
                for (JsonNode game : games) {
                    String gameId = game.path("gameId").asText();
                    if (gameId.isEmpty()) continue;

                    String gameDateTime = game.path("gameDateTime").asText("");
                    String gameTime = (gameDateTime.length() >= 16) ? gameDateTime.substring(11, 16) : "";

                    scoreMap.put(gameId, BaseballGameDto.builder()
                            .homeScore(game.path("homeTeamScore").asInt(0))
                            .awayScore(game.path("awayTeamScore").asInt(0))
                            .gameTime(gameTime)
                            .stadium(game.path("stadium").asText(""))
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("점수 조회 API 오류 ({}): {}", date, e.getMessage());
        }
        return scoreMap;
    }

    // ── KBO 공식 사이트 스크래핑 ──────────────────────────────────────────────

    /**
     * KBO 공식 사이트의 AJAX 엔드포인트에서 경기 결과를 스크래핑합니다.
     * Web Forms 방식이므로 Javascript 렌더링 우회를 위해 직접 데이터를 찌릅니다.
     */
    private List<Map<String, Object>> scrapeKboScoresByDate(String date) {
        // 월요일(휴식일) 예외 처리: 데이터 자체가 없으므로 불필요한 스크래핑 시도 및 에러 방지
        java.time.LocalDate targetDate = java.time.LocalDate.parse(date);
        if (targetDate.getDayOfWeek() == java.time.DayOfWeek.MONDAY) {
            log.info("ℹ️ {} (월요일)은 KBO 리그 정기 휴식일이므로 스크래핑을 진행하지 않습니다.", date);
            return java.util.Collections.emptyList();
        }

        String year = date.substring(0, 4); // "2026"
        String month = date.substring(5, 7); // "04"
        String shortDateStr = month + "." + date.substring(8, 10); // "04.05"

        List<Map<String, Object>> results = new ArrayList<>();
        try {
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.connect("https://www.koreabaseball.com/ws/Schedule.asmx/GetScheduleList")
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .data("leId", "1")
                    .data("srIdList", "0,9,6")
                    .data("seasonId", year)
                    .data("gameMonth", month)
                    .data("teamId", "")
                    .ignoreContentType(true)
                    .timeout(10000)
                    .post();

            String jsonResponse = doc.body().text();
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(jsonResponse);
            com.fasterxml.jackson.databind.JsonNode rows = root.path("rows");

            String currentDate = "";
            for (com.fasterxml.jackson.databind.JsonNode row : rows) {
                com.fasterxml.jackson.databind.JsonNode cells = row.path("row");
                if (cells.isEmpty()) continue;

                String matchHtml = "";
                String stadiumHtml = "";

                // 첫 번째 컬럼이 날짜(예: 04.05(수))인지 시간(예: 14:00)인지 확인하여
                // rowspan으로 인한 인덱스 밀림 현상을 완벽하게 방지
                boolean isFirstGameOfDay = false;
                String firstCellText = org.jsoup.Jsoup.parse(cells.get(0).path("Text").asText()).text().trim();
                
                if (firstCellText.matches("^\\d{2}\\.\\d{2}.*")) {
                    currentDate = firstCellText; // "04.05(일)"
                    isFirstGameOfDay = true;
                }

                if (currentDate.startsWith(shortDateStr)) {
                    if (isFirstGameOfDay && cells.size() > 7) {
                        matchHtml = cells.get(2).path("Text").asText();
                        stadiumHtml = cells.get(7).path("Text").asText();
                    } else if (!isFirstGameOfDay && cells.size() > 6) {
                        matchHtml = cells.get(1).path("Text").asText();
                        stadiumHtml = cells.get(6).path("Text").asText();
                    }
                }

                if (!matchHtml.isEmpty()) {
                    // <span>한화</span><em><span>8</span><span>vs</span><span>0</span></em><span>두산</span> -> 한화 8 vs 0 두산
                    String text = org.jsoup.Jsoup.parse(matchHtml).text().trim(); 
                    
                    // 공백 여부 무관하게 (팀명)(점수)vs(점수)(팀명) 매칭
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)(.*?) ?(\\d+) ?vs ?(\\d+) ?(.*)").matcher(text);
                    if (m.find()) {
                        Map<String, Object> game = new HashMap<>();
                        game.put("awayTeamName", m.group(1).trim());
                        game.put("awayScore", Integer.parseInt(m.group(2)));
                        game.put("homeScore", Integer.parseInt(m.group(3)));
                        game.put("homeTeamName", m.group(4).trim());
                        game.put("stadium", org.jsoup.Jsoup.parse(stadiumHtml).text().trim());
                        results.add(game);
                    }
                }
            }
            log.info("KBO 스크래핑 완료 ({}): {}경기 결과 수집됨", date, results.size());
        } catch (Exception e) {
            log.error("KBO 스코어 스크래핑 오류 ({}): {}", date, e.getMessage());
        }
        return results;
    }

    /** Naver calendar API 팀코드를 정규화 코드로 변환 */
    private String toCanonicalCode(String code) {
        return NAVER_CODE_TO_CANONICAL.getOrDefault(code, code);
    }
}