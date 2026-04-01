package com.team.deliveryservice.application.deliverymanager;

import com.team.deliveryservice.presentation.common.CurrentUser;
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
