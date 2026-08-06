package com.tails.place;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceImageRepository extends JpaRepository<PlaceImage, Long> {

    List<PlaceImage> findByPlace_PlaceIdOrderBySequenceAsc(Long placeId);

    boolean existsByPlace_PlaceId(Long placeId);
}
