package com.fullcount.dto.admin;

import com.fullcount.domain.MemberRole;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminMemberFilterForm {

    @Size(max = 50, message = "검색어는 50자 이하여야 합니다.")
    private String keyword;

    private Boolean active;

    private MemberRole role;
}
