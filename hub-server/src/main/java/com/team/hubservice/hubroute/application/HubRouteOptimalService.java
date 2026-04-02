package com.team.hubservice.hubroute.application;

import com.team.common.exception.BusinessException;
import com.team.hubservice.hubroute.domain.HubRoute;
import com.team.hubservice.hubroute.domain.HubRouteRepository;
import com.team.hubservice.hubroute.exception.HubRouteErrorCode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HubRouteOptimalService {

    private final HubRouteRepository hubRouteRepository;

    public HubRouteOptimalService(HubRouteRepository hubRouteRepository) {
        this.hubRouteRepository = hubRouteRepository;
    }

    public OptimalRouteResult findOptimalRoute(OptimalRouteQuery query) {
        List<HubRoute> allRoutes = hubRouteRepository.findAll();
        Map<UUID, List<HubRoute>> graph = buildGraph(allRoutes);

        Map<UUID, Integer> minDurationMap = new HashMap<>();
        Map<UUID, Double> minDistanceMap = new HashMap<>();
        Map<UUID, HubRoute> previousRouteMap = new HashMap<>();

        PriorityQueue<RouteNode> pq = new PriorityQueue<>(Comparator.comparingInt(RouteNode::totalDuration));

        pq.offer(new RouteNode(query.departureHubId(), 0, 0.0));
        minDurationMap.put(query.departureHubId(), 0);
        minDistanceMap.put(query.departureHubId(), 0.0);

        while (!pq.isEmpty()) {
            RouteNode current = pq.poll();

            if (current.currentHubId().equals(query.arrivalHubId())) {
                break;
            }

            if (current.totalDuration() > minDurationMap.getOrDefault(current.currentHubId(), Integer.MAX_VALUE)) {
                continue;
            }

            List<HubRoute> adjRoutes = graph.getOrDefault(current.currentHubId(), Collections.emptyList());
            for (HubRoute edge : adjRoutes) {
                int newDuration = current.totalDuration() + edge.getDuration();
                double newDistance = current.totalDistance() + edge.getDistance();

                if (newDuration < minDurationMap.getOrDefault(edge.getArrivalHubId(), Integer.MAX_VALUE)) {
                    minDurationMap.put(edge.getArrivalHubId(), newDuration);
                    minDistanceMap.put(edge.getArrivalHubId(), newDistance);
                    previousRouteMap.put(edge.getArrivalHubId(), edge);
                    pq.offer(new RouteNode(edge.getArrivalHubId(), newDuration, newDistance));
                }
            }
        }

        if (!previousRouteMap.containsKey(query.arrivalHubId())) {
            throw new BusinessException(HubRouteErrorCode.OPTIMAL_NOT_FOUND);
        }

        List<RoutePathInfo> pathList = buildPathList(previousRouteMap, query.arrivalHubId());

        return new OptimalRouteResult(
            query.departureHubId(),
            query.arrivalHubId(),
            minDurationMap.get(query.arrivalHubId()),
            minDistanceMap.get(query.arrivalHubId()),
            pathList
        );
    }

    private Map<UUID, List<HubRoute>> buildGraph(List<HubRoute> routes) {
        return routes.stream().collect(Collectors.groupingBy(HubRoute::getDepartureHubId));
    }

    private List<RoutePathInfo> buildPathList(Map<UUID, HubRoute> previousRouteMap, UUID arrivalHubId) {
        List<RoutePathInfo> pathList = new ArrayList<>();
        UUID currentId = arrivalHubId;

        while (previousRouteMap.containsKey(currentId)) {
            HubRoute route = previousRouteMap.get(currentId);
            pathList.add(new RoutePathInfo(
                0,
                route.getDepartureHubId(),
                route.getArrivalHubId(),
                route.getDuration(),
                route.getDistance()
            ));
            currentId = route.getDepartureHubId();
        }

        Collections.reverse(pathList);

        for (int i = 0; i < pathList.size(); i++) {
            RoutePathInfo oldInfo = pathList.get(i);
            pathList.set(i, new RoutePathInfo(
                i + 1,
                oldInfo.departureHubId(),
                oldInfo.arrivalHubId(),
                oldInfo.duration(),
                oldInfo.distance()
            ));
        }

        return pathList;
    }

    private record RouteNode(UUID currentHubId, int totalDuration, double totalDistance) {}
}
