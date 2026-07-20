package com.tails.pet;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.MemberRepository;
import com.tails.pet.dto.PetCreateRequest;
import com.tails.pet.dto.PetResponse;
import com.tails.pet.dto.PetUpdateRequest;
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

    @Transactional
    public void update(Long memberId, Long petId, PetUpdateRequest request) {
        Pet pet = getPetOrThrow(petId);
        requireOwner(pet, memberId);
        pet.changeInfo(request.name(), request.species(), request.birthDate());
    }

    @Transactional
    public void delete(Long memberId, Long petId) {
        Pet pet = getPetOrThrow(petId);
        requireOwner(pet, memberId);
        petRepository.delete(pet);
    }

    private void requireOwner(Pet pet, Long memberId) {
        if (!pet.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.NOT_PET_OWNER);
        }
    }

    private Pet getPetOrThrow(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new CustomException(ErrorCode.PET_NOT_FOUND));
    }
}
