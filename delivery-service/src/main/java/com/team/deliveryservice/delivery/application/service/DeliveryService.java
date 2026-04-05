package com.team.deliveryservice.delivery.application.service;

import com.team.deliveryservice.delivery.application.dto.request.AssignCompanyDeliveryManagerRequest;
import com.team.deliveryservice.delivery.application.dto.request.AssignHubDeliveryManagerRequest;
import com.team.deliveryservice.delivery.application.dto.request.ChangeDeliveryStatusRequest;
import com.team.deliveryservice.delivery.application.dto.request.CreateDeliveryRequest;
import com.team.deliveryservice.delivery.application.dto.request.UpdateDeliveryRequest;
import com.team.deliveryservice.delivery.application.dto.response.AiDeliveryResponse;
import com.team.deliveryservice.delivery.application.dto.response.DeliveryPageResponse;
import com.team.deliveryservice.delivery.application.dto.response.DeliveryResponse;
import com.team.deliveryservice.delivery.application.search.DeliverySearchCondition;
import com.team.deliveryservice.global.common.CurrentUser;
import java.util.UUID;

public interface DeliveryService {

    // external
    DeliveryResponse getDelivery(UUID deliveryId, CurrentUser currentUser);

    DeliveryPageResponse searchDeliveries(DeliverySearchCondition condition, CurrentUser currentUser);

    DeliveryResponse updateDelivery(UUID deliveryId, UpdateDeliveryRequest request, CurrentUser currentUser);

    DeliveryResponse assignCompanyDeliveryManager(
        UUID deliveryId,
        AssignCompanyDeliveryManagerRequest request,
        CurrentUser currentUser
    );

    DeliveryResponse assignHubDeliveryManager(
        UUID deliveryId,
        AssignHubDeliveryManagerRequest request,
        CurrentUser currentUser
    );

    // internal
    DeliveryResponse createDelivery(CreateDeliveryRequest request);

    DeliveryResponse getDelivery(UUID deliveryId);

    DeliveryResponse changeDeliveryStatus(UUID deliveryId, ChangeDeliveryStatusRequest request);

    DeliveryResponse cancelDelivery(UUID deliveryId);

    void deleteDelivery(UUID deliveryId);

    AiDeliveryResponse getAiDeliveryInfo(UUID deliveryId);
}
