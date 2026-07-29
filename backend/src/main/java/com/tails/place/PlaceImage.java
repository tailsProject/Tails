package com.tails.place;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

// TourAPI 이미지정보서비스(detailImage2)로 받아온 장소별 추가 사진들.
// Place.imageUrl(대표 이미지 1장)과 별개로, 상세 화면 갤러리용으로 여러 장을 저장한다.
@Entity
@Table(name = "place_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Place place;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    // TourAPI가 내려주는 순서 그대로 보존(갤러리 정렬용)
    @Column(nullable = false)
    private int sequence;

    @Builder
    public PlaceImage(Place place, String imageUrl, int sequence) {
        this.place = place;
        this.imageUrl = imageUrl;
        this.sequence = sequence;
    }
}
