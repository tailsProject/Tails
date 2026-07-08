package com.tails.place.sync.dto;

// /preview 응답 한 건. alreadyExists가 true면 detail 조회를 생략해서 detail은 null
public record PlacePreviewItem(PetTourListItem listItem, PetTourDetailItem detail, boolean alreadyExists) {
}
