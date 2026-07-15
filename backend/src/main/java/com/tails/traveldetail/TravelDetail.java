package com.tails.traveldetail;

import com.tails.place.Place;
import com.tails.travel.Travel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 여행 일정(Travel) 하나 안에서 "몇 월 며칠에 어떤 장소를 몇 시에 방문하는지" 한 줄
// (travel_id, travel_date, sequence) 조합이 DB 레벨에서 유니크 — 같은 날짜 안 방문 순서 중복 방지
@Entity
@Table(
        name = "travel_detail",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_travel_detail_travel_date_sequence",
                columnNames = {"travel_id", "travel_date", "sequence"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class TravelDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_id")
    private Long detailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_id")
    private Travel travel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @Column(name = "travel_date")
    private LocalDate travelDate;

    @Column(name = "visit_time")
    private LocalTime visitTime;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    // 같은 travel + travelDate 안에서의 방문 순서(1부터). SQL의 SEQUENCE(자동 증가 생성기)와는
    // 무관한 평범한 정수 컬럼 — 값은 Service에서 직접 계산해서 채움
    @Column(name = "sequence")
    private Integer sequence;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public TravelDetail(Travel travel, Place place, LocalDate travelDate, LocalTime visitTime,
                         String memo, Integer sequence) {
        this.travel = travel;
        this.place = place;
        this.travelDate = travelDate;
        this.visitTime = visitTime;
        this.memo = memo;
        this.sequence = sequence;
    }

    // travelDate/place/sequence는 여기서 안 바꿈 — 이 셋은 유니크 제약과 얽혀 있어서 바꾸려면
    // 다른 방문들과 순서가 안 겹치게 재배치하는 별도 로직이 필요함 (나중에 순서 재정렬 기능과 함께 처리)
    public void updateInfo(LocalTime visitTime, String memo) {
        this.visitTime = visitTime;
        this.memo = memo;
    }

    // 방문 순서만 변경. (travel_id, travel_date, sequence) 유니크 제약 때문에 호출하는 쪽이
    // "전부 임시 음수값으로 옮긴 뒤 최종값으로 재배치"하는 2단계 절차를 책임져야 함
    public void changeSequence(Integer sequence) {
        this.sequence = sequence;
    }
}
