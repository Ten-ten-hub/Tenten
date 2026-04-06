package com.team.hubservice.hubroute.infrastructure.tmap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TmapRoutesResponse {

    @JsonProperty("features")
    private List<TmapFeature> features;

    public List<TmapFeature> getFeatures() {
        return features;
    }

    public void setFeatures(List<TmapFeature> features) {
        this.features = features;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TmapFeature {

        @JsonProperty("properties")
        private TmapFeatureProperties properties;

        public TmapFeatureProperties getProperties() {
            return properties;
        }

        public void setProperties(TmapFeatureProperties properties) {
            this.properties = properties;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TmapFeatureProperties {

        @JsonProperty("totalDistance")
        private Integer totalDistance;

        @JsonProperty("totalTime")
        private Integer totalTime;

        public Integer getTotalDistance() {
            return totalDistance;
        }

        public void setTotalDistance(Integer totalDistance) {
            this.totalDistance = totalDistance;
        }

        public Integer getTotalTime() {
            return totalTime;
        }

        public void setTotalTime(Integer totalTime) {
            this.totalTime = totalTime;
        }
    }
}
