package com.tails.place.sync.dto;

import java.util.List;

// /category-counts 응답. estimatedDays는 하루 1000건 처리 가정, 올림 계산
public record CategoryCountSummaryResponse(
        List<CategoryCountItem> categories, long totalCount, long estimatedDays) {
}
