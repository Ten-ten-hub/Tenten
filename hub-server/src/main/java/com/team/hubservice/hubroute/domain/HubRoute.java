package com.team.hubservice.hubroute.domain;

import com.team.common.BaseEntity;
import com.team.common.exception.BusinessException;
import com.team.hubservice.global.exception.HubRouteErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "p_hub_route")
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("deleted_at IS NULL")
public class HubRoute extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "departure_hub_id", nullable = false)
    private UUID departureHubId;

    @Column(name = "arrival_hub_id", nullable = false)
    private UUID arrivalHubId;

    @Column(nullable = false)
    private Integer duration;

    @Column(nullable = false)
    private Double distance;

    protected HubRoute() {
    }

    private HubRoute(UUID departureHubId, UUID arrivalHubId, Integer duration, Double distance) {
        validateHubs(departureHubId, arrivalHubId);
        validateMetrics(duration, distance);

        this.departureHubId = departureHubId;
        this.arrivalHubId = arrivalHubId;
        this.duration = duration;
        this.distance = distance;
    }

    public static HubRoute create(UUID departureHubId, UUID arrivalHubId, Integer duration, Double distance) {
        return new HubRoute(departureHubId, arrivalHubId, duration, distance);
    }

    public void update(Integer duration, Double distance) {
        if (duration != null || distance != null) {
            validateMetrics(
                duration != null ? duration : this.duration,
                distance != null ? distance : this.distance
            );
        }
        if (duration != null) this.duration = duration;
        if (distance != null) this.distance = distance;
    }

    private void validateHubs(UUID departureHubId, UUID arrivalHubId) {
        if (departureHubId.equals(arrivalHubId)) {
            throw new BusinessException(HubRouteErrorCode.SAME_HUB_NOT_ALLOWED);
        }
    }

    private void validateMetrics(Integer duration, Double distance) {
        if (duration < 0 || distance < 0) {
            throw new BusinessException(HubRouteErrorCode.NEGATIVE_VALUE_NOT_ALLOWED);
        }
    }

    public UUID getId() { return id; }
    public UUID getDepartureHubId() { return departureHubId; }
    public UUID getArrivalHubId() { return arrivalHubId; }
    public Integer getDuration() { return duration; }
    public Double getDistance() { return distance; }
}
