package com.team.hubservice.hubroute.presentation.dto;

import com.team.hubservice.hubroute.application.TmapHubRouteBulkSyncResult;
import java.util.List;

public record TmapHubRouteBulkSyncResponse(
    int attempted,
    int succeeded,
    int failed,
    List<String> errors
) {

    public static TmapHubRouteBulkSyncResponse from(TmapHubRouteBulkSyncResult result) {
        return new TmapHubRouteBulkSyncResponse(
            result.attempted(),
            result.succeeded(),
            result.failed(),
            List.copyOf(result.errors())
        );
    }
}
