package com.fullcount;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScrapeTest {

    @Test
    public void testScrape() throws Exception {
        String year = "2026";
        String month = "04";

        System.out.println("Fetching AJAX endpoint...");
        Document doc = Jsoup.connect("https://www.koreabaseball.com/ws/Schedule.asmx/GetScheduleList")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("X-Requested-With", "XMLHttpRequest")
                .data("leId", "1")
                .data("srIdList", "0,9,6")
                .data("seasonId", year)
                .data("gameMonth", month)
                .data("teamId", "")
                .ignoreContentType(true)
                .post();

        System.out.println(doc.body().text().substring(0, Math.min(500, doc.body().text().length())));
    }
}
