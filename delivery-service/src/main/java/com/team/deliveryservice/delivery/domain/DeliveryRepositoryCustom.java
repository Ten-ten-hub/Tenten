package com.team.deliveryservice.delivery.domain;

import com.team.deliveryservice.delivery.application.search.DeliverySearchCondition;
import org.springframework.data.domain.Page;

public interface DeliveryRepositoryCustom {
    Page<Delivery> search(DeliverySearchCondition condition, int size);
}
