package com.team.aiservice.infrastructure.client;

import com.team.aiservice.infrastructure.config.NaverFeignConfig;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "naver-news", url = "https://openapi.naver.com", configuration = NaverFeignConfig.class)
public interface NaverNewsClient {
    @GetMapping("/v1/search/news.json")
    NaverNewsResponse searchNews(@RequestParam("query") String query,
                                 @RequestParam("display") int display);

    record NaverNewsResponse(List<NewsItem> items) {
    }

    record NewsItem(String title, String description) {
    }
}
