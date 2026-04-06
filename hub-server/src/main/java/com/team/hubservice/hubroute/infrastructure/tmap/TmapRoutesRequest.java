package com.team.hubservice.hubroute.infrastructure.tmap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TmapRoutesRequest {

    @JsonProperty("startX")
    private final String startX;

    @JsonProperty("startY")
    private final String startY;

    @JsonProperty("endX")
    private final String endX;

    @JsonProperty("endY")
    private final String endY;

    @JsonProperty("reqCoordType")
    private final String reqCoordType = "WGS84GEO";

    @JsonProperty("resCoordType")
    private final String resCoordType = "WGS84GEO";

    @JsonProperty("searchOption")
    private final String searchOption = "0";

    public TmapRoutesRequest(String startX, String startY, String endX, String endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    public String getStartX() {
        return startX;
    }

    public String getStartY() {
        return startY;
    }

    public String getEndX() {
        return endX;
    }

    public String getEndY() {
        return endY;
    }

    public String getReqCoordType() {
        return reqCoordType;
    }

    public String getResCoordType() {
        return resCoordType;
    }

    public String getSearchOption() {
        return searchOption;
    }
}
