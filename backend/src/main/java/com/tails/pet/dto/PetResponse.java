package com.tails.pet.dto;

import com.tails.pet.Pet;
import java.time.LocalDate;

// 반려동물 응답 - 단건 조회(PetController)와 MemberResponse 목록 항목에서 공통 사용
public record PetResponse(Long petId, String name, String species, LocalDate birthDate) {
    public static PetResponse from(Pet pet) {
        return new PetResponse(pet.getId(), pet.getName(), pet.getSpecies(), pet.getBirthDate());
    }
}
