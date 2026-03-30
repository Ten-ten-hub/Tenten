package com.team.deliveryservice.application.delivery;

import java.util.List;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
public record DeliveryPageResponse(
    List<DeliveryResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {
    public static DeliveryPageResponse from(Page<DeliveryResponse> page) {
        return DeliveryPageResponse.builder()
            .content(page.getContent())
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .hasNext(page.hasNext())
            .build();
    }
}
