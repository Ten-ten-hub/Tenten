package com.team.hubservice.hubroute.infrastructure.tmap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tmap")
public class TmapProperties {

    /**
     * SK 오픈API 호스트
     */
    private String baseUrl = "https://apis.openapi.sk.com";

    /**
     * 티맵 개발자 센터 앱 키 HTTP 헤더 appKey로 전달
     */
    private String appKey = "";

    /**
     * true면 {@code /tmap/truck/routes} 사용, false면 {@code /tmap/routes} 사용
     */
    private boolean useTruckRoutes = false;

    /**
     * 전체 허브 쌍 동기화 시 호출 간 지연(ms) API 과호출 완화용
     */
    private long requestDelayMs = 0;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public boolean isUseTruckRoutes() {
        return useTruckRoutes;
    }

    public void setUseTruckRoutes(boolean useTruckRoutes) {
        this.useTruckRoutes = useTruckRoutes;
    }

    public long getRequestDelayMs() {
        return requestDelayMs;
    }

    public void setRequestDelayMs(long requestDelayMs) {
        this.requestDelayMs = requestDelayMs;
    }
}
