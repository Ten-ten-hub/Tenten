package com.team.hubservice.hubroute.application;

import java.util.List;

public record TmapHubRouteBulkSyncResult(
    int attempted,
    int succeeded,
    int failed,
    List<String> errors
) {
}
