package com.tails.place;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * PLACE 테이블 매핑 엔티티.
 * placeId/createdAt/updatedAt은 DB/Auditing이 채우는 값이라 빌더 파라미터에서 제외
 */
@Entity
@Table(name = "place")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id")
    private Long placeId;

    /** 한국관광공사 API의 contentid — 중복 저장 방지용 */
    @Column(name = "external_place_id", nullable = false, unique = true)
    private String externalPlaceId;

    @Column(name = "content_type_id")
    private String contentTypeId;

    @Column(name = "place_name", nullable = false)
    private String placeName;

    @Column(name = "cat1")
    private String cat1;

    @Column(name = "cat2")
    private String cat2;

    @Column(name = "cat3")
    private String cat3;

    @Column(name = "address")
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "phone")
    private String phone;

    @Column(name = "pet_info", columnDefinition = "TEXT")
    private String petInfo;

    @Column(name = "image_url")
    private String imageUrl;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Place(String externalPlaceId, String contentTypeId, String placeName, String cat1, String cat2,
                 String cat3, String address, Double latitude, Double longitude, String phone,
                 String petInfo, String imageUrl) {
        this.externalPlaceId = externalPlaceId;
        this.contentTypeId = contentTypeId;
        this.placeName = placeName;
        this.cat1 = cat1;
        this.cat2 = cat2;
        this.cat3 = cat3;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phone = phone;
        this.petInfo = petInfo;
        this.imageUrl = imageUrl;
    }
}
