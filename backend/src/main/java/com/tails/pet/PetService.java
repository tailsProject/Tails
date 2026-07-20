package com.tails.pet;

import com.tails.member.MemberRepository;
import com.tails.pet.dto.PetCreateRequest;
import com.tails.pet.dto.PetResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 반려동물 CRUD 비즈니스 로직
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {

    private final PetRepository petRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Long create(Long memberId, PetCreateRequest request) {
        Pet pet = Pet.builder()
                .member(memberRepository.getReferenceById(memberId))
                .name(request.name())
                .species(request.species())
                .birthDate(request.birthDate())
                .build();
        return petRepository.save(pet).getId();
    }

    public List<PetResponse> getMyPets(Long memberId) {
        return petRepository.findByMemberId(memberId).stream().map(PetResponse::from).toList();
    }
}
