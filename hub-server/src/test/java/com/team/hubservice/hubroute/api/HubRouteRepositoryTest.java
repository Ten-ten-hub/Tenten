package com.team.hubservice.hubroute.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.team.hubservice.hub.domain.Hub;
import com.team.hubservice.hub.domain.HubRepository;
import com.team.hubservice.hubroute.domain.HubRoute;
import com.team.hubservice.hubroute.domain.HubRouteRepository;
import com.team.hubservice.hubroute.presentation.dto.AiRouteResponse;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class HubRouteRepositoryTest {

    @Autowired
    private HubRouteRepository hubRouteRepository;

    @Autowired
    private HubRepository hubRepository;

    @Test
    @DisplayName("출발지와 도착지 ID로 경로 및 위경도 조인 조회 쿼리가 정상 작동합니다.")
    void findRouteWithCoordinatesTest() {
        // given
        Hub originHub = Hub.create("서울 출발 허브", "서울 송파구", 37.5665, 126.9780);
        Hub destHub = Hub.create("부산 도착 허브", "부산 연제구", 35.1796, 129.0756);

        hubRepository.save(originHub);
        hubRepository.save(destHub);

        HubRoute route = HubRoute.create(originHub.getId(), destHub.getId(), 180, 385.5);
        hubRouteRepository.save(route);

        // when
        Optional<AiRouteResponse> responseOpt = hubRouteRepository.findRouteWithCoordinates(originHub.getId(), destHub.getId());

        // then
        assertThat(responseOpt).isPresent();

        AiRouteResponse response = responseOpt.get();
        assertThat(response.duration()).isEqualTo(180);
        assertThat(response.distance()).isEqualTo(385.5);
        assertThat(response.originLat()).isEqualTo(37.5665);
        assertThat(response.originLng()).isEqualTo(126.9780);
        assertThat(response.destLat()).isEqualTo(35.1796);
        assertThat(response.destLng()).isEqualTo(129.0756);
    }
}
