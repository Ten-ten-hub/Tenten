package com.team.hubservice.hubroute.infrastructure.tmap;

import com.team.common.exception.BusinessException;
import com.team.hubservice.global.exception.HubRouteErrorCode;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class TmapRouteClient {

    private final RestTemplate tmapRestTemplate;
    private final TmapProperties tmapProperties;

    public TmapRouteClient(@Qualifier("tmapRestTemplate") RestTemplate tmapRestTemplate, TmapProperties tmapProperties) {
        this.tmapRestTemplate = tmapRestTemplate;
        this.tmapProperties = tmapProperties;
    }

    @PostConstruct
    void logTmapAppKeyPresence() {
        String key = tmapProperties.getAppKey();
        int len = key == null ? 0 : key.trim().length();
        log.info("tmap.app-key loaded? length={}", len);
    }

    /**
     * 티맵 경로 API 호출 후 features[0].properties 의 totalDistance(m), totalTime(s)만 반환
     */
    public TmapRouteMetrics fetchDrivingMetrics(double startLng, double startLat, double endLng, double endLat) {
        if (!StringUtils.hasText(tmapProperties.getAppKey())) {
            throw new BusinessException(HubRouteErrorCode.TMAP_APP_KEY_MISSING);
        }

        String path = tmapProperties.isUseTruckRoutes() ? "/tmap/truck/routes" : "/tmap/routes";
        String url = UriComponentsBuilder.fromUriString(tmapProperties.getBaseUrl() + path)
            .queryParam("version", 1)
            .queryParam("format", "json")
            .build(true)
            .toUriString();

        TmapRoutesRequest requestPayload = new TmapRoutesRequest(
            Double.toString(startLng),
            Double.toString(startLat),
            Double.toString(endLng),
            Double.toString(endLat)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("appKey", tmapProperties.getAppKey());

        try {
            ResponseEntity<TmapRoutesResponse> response = tmapRestTemplate.postForEntity(
                url,
                new HttpEntity<>(requestPayload, headers),
                TmapRoutesResponse.class
            );
            return parseFirstFeatureMetrics(response.getBody());
        } catch (RestClientException e) {
            Map<String, Object> details = new LinkedHashMap<>();
            if (e instanceof HttpStatusCodeException httpEx) {
                String bodyPreview = truncate(httpEx.getResponseBodyAsString(StandardCharsets.UTF_8), 1200);
                details.put("httpStatus", httpEx.getStatusCode().value());
                details.put("responseBody", bodyPreview);
                log.warn("Tmap API HTTP 오류 status={} bodyPreview={}", httpEx.getStatusCode(), bodyPreview);
            } else {
                details.put("message", e.getMessage());
                log.warn("Tmap API 호출 실패", e);
            }
            throw new BusinessException(HubRouteErrorCode.TMAP_REQUEST_FAILED, e);
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) {
            return "";
        }
        String trimmed = s.trim();
        if (trimmed.length() <= maxLen) {
            return trimmed;
        }
        return trimmed.substring(0, maxLen) + "...(truncated)";
    }

    private static TmapRouteMetrics parseFirstFeatureMetrics(TmapRoutesResponse body) {
        if (body == null || body.getFeatures() == null || body.getFeatures().isEmpty()) {
            throw new BusinessException(HubRouteErrorCode.TMAP_RESPONSE_INVALID);
        }
        TmapRoutesResponse.TmapFeature firstFeature = body.getFeatures().get(0);
        if (firstFeature == null) {
            throw new BusinessException(HubRouteErrorCode.TMAP_RESPONSE_INVALID);
        }

        TmapRoutesResponse.TmapFeatureProperties props = body.getFeatures().get(0).getProperties();
        if (props == null || props.getTotalDistance() == null || props.getTotalTime() == null) {
            throw new BusinessException(HubRouteErrorCode.TMAP_RESPONSE_INVALID);
        }
        return TmapRouteMetrics.fromTmapTotals(props.getTotalTime(), props.getTotalDistance());
    }
}
