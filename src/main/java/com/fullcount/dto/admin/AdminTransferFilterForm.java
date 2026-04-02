package com.fullcount.dto.admin;

import com.fullcount.domain.TransferStatus;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminTransferFilterForm {

    @Size(max = 50, message = "검색어는 50자 이하여야 합니다.")
    private String keyword;

    private TransferStatus status;
}
