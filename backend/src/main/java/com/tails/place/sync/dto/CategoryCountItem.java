package com.tails.place.sync.dto;

// TourAPI 카테고리 하나의 전체 건수
public record CategoryCountItem(String contentTypeId, String label, long totalCount) {
}
