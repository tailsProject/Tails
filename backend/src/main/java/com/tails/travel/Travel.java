package com.tails.travel;

import com.tails.member.Member;
import com.tails.traveldetail.TravelDetail;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 여행 일정 하나. 방문 장소/시간 등 세부 내용은 TravelDetail(1:N)이 담당
// Lombok/JPA 어노테이션 컨벤션은 Place와 동일
@Entity
@Table(name = "travel")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Travel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "travel_id")
    private Long travelId;

    // Board.member(nullable=true, 탈퇴해도 글은 남김)와 달리 nullable=false — 개인 일정이라
    // 소유자 없이는 의미가 없고, 탈퇴 시 Member.travels의 cascade=ALL로 함께 삭제
    // (MemberService.withdraw가 엔티티를 로드해서 지우는 방식이라 JPA cascade만으로 충분)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // cascade=ALL + orphanRemoval=true: Travel이 지워질 때 딸린 TravelDetail도 함께 지워서
    // 고아 데이터(travel_id가 없는 travel_detail 행)가 안 남게 함
    @OneToMany(mappedBy = "travel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TravelDetail> travelDetails = new ArrayList<>();

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Travel(Member member, String title, LocalDate startDate, LocalDate endDate) {
        this.member = member;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void updateInfo(String title, LocalDate startDate, LocalDate endDate) {
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
