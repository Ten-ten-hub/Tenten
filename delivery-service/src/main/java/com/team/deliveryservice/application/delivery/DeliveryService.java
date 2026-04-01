package com.team.deliveryservice.application.delivery;

import com.team.deliveryservice.presentation.common.CurrentUser;
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
