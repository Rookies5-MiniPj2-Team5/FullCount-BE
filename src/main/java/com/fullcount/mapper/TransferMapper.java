package com.fullcount.mapper;

import com.fullcount.domain.*;
import org.springframework.stereotype.Component;

@Component
public class TransferMapper {

    /** 양도 요청 Transfer 엔티티 생성 */
    public static Transfer toEntity(Post post, Member buyer) {
        return Transfer.builder()
                .post(post)
                .seller(post.getAuthor())
                .buyer(buyer)
                .price(post.getTicketPrice() != null ? post.getTicketPrice() : 0)
                .build();
    }
}