package com.team.hubservice.hubroute.infrastructure.tmap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.team.common.exception.BusinessException;
import com.team.hubservice.global.exception.HubRouteErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class TmapRouteClientTest {

    private MockRestServiceServer server;
    private TmapRouteClient client;
    private TmapProperties properties;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        properties = new TmapProperties();
        properties.setBaseUrl("http://localhost:9");
        properties.setAppKey("unit-test-app-key");
        properties.setUseTruckRoutes(false);
        client = new TmapRouteClient(restTemplate, properties);
    }

    @Test
    void postsJsonWithAppKeyHeaderAndParsesFirstFeature() {
        String json = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "properties": { "totalDistance": 18500, "totalTime": 2400 }
                }
              ]
            }
            """;

        server.expect(requestTo("http://localhost:9/tmap/routes?version=1&format=json"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("appKey", "unit-test-app-key"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        TmapRouteMetrics metrics = client.fetchDrivingMetrics(127.1, 37.4, 127.0, 37.5);
        assertThat(metrics.durationMinutes()).isEqualTo(40);
        assertThat(metrics.distanceKm()).isEqualTo(18.5);
        server.verify();
    }

    @Test
    void throwsWhenAppKeyMissing() {
        properties.setAppKey("");
        assertThatThrownBy(() -> client.fetchDrivingMetrics(0, 0, 1, 1))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(HubRouteErrorCode.TMAP_APP_KEY_MISSING);
    }
}
