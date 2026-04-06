package com.team.aiservice.application.service;

import com.team.aiservice.infrastructure.client.NaverNewsClient;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverNewsService {

    private final NaverNewsClient naverNewsClient;

    @Cacheable(cacheNames = "naverNews", key = "#query")
    public String fetchNewsContext(String query) {
        log.info("[CACHE MISS] 레디스에 데이터가 없거나 만료되었습니다. 네이버 뉴스를 직접 검색합니다. Query: {}", query);
        try {
            var res = naverNewsClient.searchNews(query, 10);
            if (res != null && res.items() != null && !res.items().isEmpty()) {
                return res.items().stream()
                    .map(i -> i.title().replaceAll("<[^>]*>", ""))
                    .collect(Collectors.joining(" / "));
            }
        } catch (Exception e) {
            log.error("[NAVER NEWS SEARCH] Failed: {}", e.getMessage());
        }
        return "주변 특이사항 없음";
    }
}
