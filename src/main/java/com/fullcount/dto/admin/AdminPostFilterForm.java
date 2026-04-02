package com.fullcount.dto.admin;

import com.fullcount.domain.BoardType;
import com.fullcount.domain.PostStatus;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminPostFilterForm {

    @Size(max = 50, message = "검색어는 50자 이하여야 합니다.")
    private String keyword;

    private BoardType boardType;

    private PostStatus status;
}
