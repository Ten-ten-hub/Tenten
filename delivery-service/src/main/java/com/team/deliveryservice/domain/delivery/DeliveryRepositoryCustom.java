package com.team.deliveryservice.domain.delivery;

import com.team.deliveryservice.application.delivery.DeliveryPageResponse;
import com.team.deliveryservice.application.delivery.DeliverySearchCondition;
import org.springframework.data.domain.Page;

public interface DeliveryRepositoryCustom {
    Page<Delivery> search(DeliverySearchCondition condition, int size);
}
