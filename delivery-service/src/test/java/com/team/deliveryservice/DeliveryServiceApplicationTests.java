package com.team.deliveryservice;


import com.team.deliveryservice.application.delivery.DeliveryService;
import com.team.deliveryservice.application.delivery.DeliveryServiceImpl;
import com.team.deliveryservice.domain.delivery.DeliveryRepository;
import com.team.deliveryservice.domain.delivery.DeliveryRouteLogRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@Import(DeliveryServiceImpl.class)
class DeliveryServiceApplicationTests {

    @Autowired
    private DeliveryService deliveryService;

    @MockitoBean
    private DeliveryRepository deliveryRepository;

    @MockitoBean
    private DeliveryRouteLogRepository deliveryRouteLogRepository;
}
