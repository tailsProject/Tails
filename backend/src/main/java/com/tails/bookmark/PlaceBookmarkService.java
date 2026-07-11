package com.tails.bookmark;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.MemberRepository;
import com.tails.place.Place;
import com.tails.place.PlaceRepository;
import com.tails.place.dto.PlaceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 장소 찜 등록 및 해제 비즈니스 로직 처리
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceBookmarkService {

    private final PlaceBookmarkRepository placeBookmarkRepository;
    private final PlaceRepository placeRepository;
    private final MemberRepository memberRepository;

    // 이미 찜했으면 취소, 안 했으면 추가
    @Transactional
    public boolean toggleBookmark(Long memberId, Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLACE_NOT_FOUND));

        return placeBookmarkRepository.findByPlace_PlaceIdAndMemberId(placeId, memberId)
                .map(existing -> {
                    placeBookmarkRepository.delete(existing);
                    return false;
                })
                .orElseGet(() -> {
                    PlaceBookmark bookmark = PlaceBookmark.builder()
                            .place(place)
                            .member(memberRepository.getReferenceById(memberId))
                            .build();
                    placeBookmarkRepository.save(bookmark);
                    return true;
                });
    }

    // 내가 찜한 장소 목록 조회 (최근 찜 순)
    public Page<PlaceResponse> getMyBookmarks(Long memberId, Pageable pageable) {
        return placeBookmarkRepository.findBookmarkedPlacesByMemberId(memberId, pageable)
                .map(PlaceResponse::from);
    }
}
