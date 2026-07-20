package com.tails.pet;

import com.tails.member.Member;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 반려동물 엔티티
@Entity
@Table(name = "pet")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pet_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 50)
    private String name;

    // 강아지/고양이 등 - 자유 입력 텍스트
    @Column(length = 30)
    private String species;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Builder
    public Pet(Member member, String name, String species, LocalDate birthDate) {
        this.member = member;
        this.name = name;
        this.species = species;
        this.birthDate = birthDate;
    }

    public void changeInfo(String name, String species, LocalDate birthDate) {
        this.name = name;
        this.species = species;
        this.birthDate = birthDate;
    }
}
