package com.team.deliveryservice.application.deliverymanager;

import java.util.List;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
public record DeliveryManagerPageResponse(
    List<DeliveryManagerResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {
    public static DeliveryManagerPageResponse from(Page<DeliveryManagerResponse> page) {
        return DeliveryManagerPageResponse.builder()
            .content(page.getContent())
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .hasNext(page.hasNext())
            .build();
    }
}
