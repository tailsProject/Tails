package com.tails.pet;

import com.tails.member.Member;
import com.tails.member.MemberRepository;
import com.tails.support.AbstractIntegrationTest;
import com.tails.travel.Travel;
import com.tails.travel.TravelRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatCode;

// 회귀테스트: travel_pet은 cascade 없는 단순 다대다라, PetService.delete()가 조인 테이블을 먼저
// 정리하지 않으면 여행에 추가된 반려동물의 삭제가 FK 위반(DataIntegrityViolationException)으로 실패한다
class PetDeleteTravelJoinIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PetRepository petRepository;
    @Autowired
    private TravelRepository travelRepository;
    @Autowired
    private PetService petService;

    @Test
    @Transactional
    void 여행에_추가된_반려동물도_삭제할_수_있다() {
        Member member = memberRepository.save(Member.builder()
                .email("pet-travel-delete@test.com")
                .password("x")
                .nickname("pettraveldeleter")
                .build());

        Pet pet = petRepository.save(Pet.builder()
                .member(member)
                .name("초코")
                .species("강아지")
                .build());

        Travel travel = Travel.builder()
                .member(member)
                .title("travel with pet")
                .description(null)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .build();
        travel.updatePets(List.of(pet));
        travelRepository.save(travel);

        assertThatCode(() -> petService.delete(member.getId(), pet.getId())).doesNotThrowAnyException();
    }
}
