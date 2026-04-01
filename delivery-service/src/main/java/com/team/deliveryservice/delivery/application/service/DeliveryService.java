package com.team.deliveryservice.delivery.application.service;

import com.team.deliveryservice.delivery.application.search.DeliverySearchCondition;
import com.team.deliveryservice.delivery.application.dto.request.*;
import com.team.deliveryservice.delivery.application.dto.response.DeliveryPageResponse;
import com.team.deliveryservice.delivery.application.dto.response.DeliveryResponse;
import com.team.deliveryservice.global.common.CurrentUser;
import java.util.UUID;

public interface DeliveryService {

    DeliveryResponse createDelivery(CreateDeliveryRequest request, CurrentUser currentUser);

    DeliveryResponse getDelivery(UUID deliveryId, CurrentUser currentUser);

    DeliveryPageResponse searchDeliveries(DeliverySearchCondition condition, CurrentUser currentUser);

    DeliveryResponse updateDelivery(UUID deliveryId, UpdateDeliveryRequest request, CurrentUser currentUser);

    DeliveryResponse changeDeliveryStatus(UUID deliveryId, ChangeDeliveryStatusRequest request, CurrentUser currentUser);

    DeliveryResponse cancelDelivery(UUID deliveryId, CurrentUser currentUser);

    DeliveryResponse assignCompanyDeliveryManager(UUID deliveryId, AssignCompanyDeliveryManagerRequest request, CurrentUser currentUser);

    DeliveryResponse assignHubDeliveryManager(UUID deliveryId, AssignHubDeliveryManagerRequest request, CurrentUser currentUser);

    void deleteDelivery(UUID deliveryId, CurrentUser currentUser);
}
