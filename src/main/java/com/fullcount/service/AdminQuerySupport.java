package com.fullcount.service;

import com.fullcount.domain.Member;
import com.fullcount.domain.Post;
import com.fullcount.domain.Transfer;
import com.fullcount.exception.BusinessException;
import com.fullcount.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class AdminQuerySupport {

    public String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    public <T> List<T> sortByPageOrder(List<T> rows, List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> order = IntStream.range(0, ids.size())
                .boxed()
                .collect(Collectors.toMap(ids::get, i -> i));

        return rows.stream()
                .sorted(Comparator.comparingInt(row -> order.get(extractId(row))))
                .toList();
    }

    private Long extractId(Object row) {
        if (row instanceof Member member) {
            return member.getId();
        }
        if (row instanceof Post post) {
            return post.getId();
        }
        if (row instanceof Transfer transfer) {
            return transfer.getId();
        }

        throw new BusinessException(
                ErrorCode.ADMIN_UNSUPPORTED_ROW_TYPE,
                "관리자 페이지 정렬 중 지원하지 않는 데이터 형식입니다: " + row.getClass().getName()
        );
    }
}
