package com.tails.member.dto;

public record MyStatsResponse(
        long travelCount,
        long bookmarkCount,
        long reviewCount
) {
}
