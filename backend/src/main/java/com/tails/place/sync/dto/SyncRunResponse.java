package com.tails.place.sync.dto;

import java.util.List;

// /run 실행 결과 요약. totalFetched == newlySaved + skippedAlreadyExists + failed
public record SyncRunResponse(
        String requestedContentType,
        int pageNo,
        int numOfRows,
        int totalFetched,
        int newlySaved,
        int skippedAlreadyExists,
        int failed,
        List<String> failedDetails) {
}
