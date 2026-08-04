package com.tails.travel;

import com.tails.member.Member;
import com.tails.pet.Pet;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 여행 일정 엔티티, 방문 장소와 시간 등 세부 내용은 TravelDetail이 담당
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

    // 개인 일정이라 소유자 필수, 회원 탈퇴 시 cascade로 함께 삭제
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 여행 삭제 시 딸린 세부 일정도 함께 삭제해 고아 데이터 방지
    @OneToMany(mappedBy = "travel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TravelDetail> travelDetails = new ArrayList<>();

    @Column(name = "title", nullable = false)
    private String title;

    // 여행 카드와 상세 상단에 보여줄 짧은 소개글, 선택 입력
    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    // 이 여행에 함께 가는 반려동물 목록, 참조만 하므로 cascade 없는 단순 다대다
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "travel_pet",
            joinColumns = @JoinColumn(name = "travel_id"),
            inverseJoinColumns = @JoinColumn(name = "pet_id")
    )
    private List<Pet> pets = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 공유 링크용 토큰, null이면 공유 안 함
    @Column(name = "share_token", unique = true, length = 36)
    private String shareToken;

    @Builder
    public Travel(Member member, String title, String description, LocalDate startDate, LocalDate endDate) {
        this.member = member;
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void updateInfo(String title, String description, LocalDate startDate, LocalDate endDate) {
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // 동반 반려동물 목록을 통째로 교체, 부분 추가나 삭제 API는 두지 않음
    public void updatePets(List<Pet> pets) {
        this.pets.clear();
        this.pets.addAll(pets);
    }

    // 공유 링크 발급, 이미 공유 중이면 기존 토큰 그대로 반환
    public String generateShareToken() {
        if (this.shareToken == null) {
            this.shareToken = UUID.randomUUID().toString();
        }
        return this.shareToken;
    }

    // 공유 중단, 비공개로 전환
    public void revokeShareToken() {
        this.shareToken = null;
    }
}
