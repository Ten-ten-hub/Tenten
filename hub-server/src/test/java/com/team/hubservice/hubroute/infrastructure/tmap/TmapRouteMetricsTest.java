package com.team.hubservice.hubroute.infrastructure.tmap;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TmapRouteMetricsTest {

    @Test
    void convertsSecondsToRoundedMinutesAndMetersToKm() {
        TmapRouteMetrics m = TmapRouteMetrics.fromTmapTotals(2400, 18500);
        assertThat(m.durationMinutes()).isEqualTo(40);
        assertThat(m.distanceKm()).isEqualTo(18.5);
    }

    @Test
    void roundsMinutesHalfUp() {
        assertThat(TmapRouteMetrics.fromTmapTotals(90, 1000).durationMinutes()).isEqualTo(2);
        assertThat(TmapRouteMetrics.fromTmapTotals(30, 1000).durationMinutes()).isEqualTo(1);
    }
}
