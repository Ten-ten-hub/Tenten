package com.team.deliveryservice.deliverymanager.domain;

import com.team.deliveryservice.deliverymanager.application.search.DeliveryManagerSearchCondition;
import com.team.deliveryservice.global.common.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeliveryManagerRepositoryCustom {
    Page<DeliveryManager> search(
        DeliveryManagerSearchCondition condition,
        CurrentUser currentUser,
        Pageable pageable
    );
}
