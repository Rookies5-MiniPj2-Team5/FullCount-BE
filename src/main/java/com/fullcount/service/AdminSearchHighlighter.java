package com.fullcount.service;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("adminSearchHighlighter")
public class AdminSearchHighlighter {

    public String highlight(String text, String keyword) {
        if (text == null) {
            return "";
        }
        if (keyword == null || keyword.isBlank()) {
            return HtmlUtils.htmlEscape(text);
        }

        String escapedText = HtmlUtils.htmlEscape(text);
        String escapedKeyword = HtmlUtils.htmlEscape(keyword.trim());
        Pattern pattern = Pattern.compile(Pattern.quote(escapedKeyword), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(escapedText);
        return matcher.replaceAll("<mark class=\"search-hit\">$0</mark>");
    }
}
