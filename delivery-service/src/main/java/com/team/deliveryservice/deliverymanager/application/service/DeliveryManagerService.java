package com.team.deliveryservice.deliverymanager.application.service;

import com.team.deliveryservice.deliverymanager.application.dto.request.CreateDeliveryManagerRequest;
import com.team.deliveryservice.deliverymanager.application.dto.request.UpdateDeliveryManagerRequest;
import com.team.deliveryservice.deliverymanager.application.dto.response.DeliveryManagerPageResponse;
import com.team.deliveryservice.deliverymanager.application.dto.response.DeliveryManagerResponse;
import com.team.deliveryservice.deliverymanager.application.search.DeliveryManagerSearchCondition;
import com.team.deliveryservice.global.common.CurrentUser;
import java.util.UUID;

public interface DeliveryManagerService {

    DeliveryManagerResponse createDeliveryManager(CreateDeliveryManagerRequest request, CurrentUser currentUser);

    DeliveryManagerResponse getDeliveryManager(UUID deliveryManagerId, CurrentUser currentUser);

    DeliveryManagerPageResponse searchDeliveryManagers(DeliveryManagerSearchCondition condition, CurrentUser currentUser);

    DeliveryManagerResponse updateDeliveryManager(
        UUID deliveryManagerId,
        UpdateDeliveryManagerRequest request,
        CurrentUser currentUser
    );

    void deleteDeliveryManager(UUID deliveryManagerId, CurrentUser currentUser);
}
