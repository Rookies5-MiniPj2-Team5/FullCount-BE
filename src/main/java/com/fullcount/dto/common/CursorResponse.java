package com.fullcount.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CursorResponse<T> {
    private List<T> content;
    private Long nextCursor;
    private boolean hasNext;
}
