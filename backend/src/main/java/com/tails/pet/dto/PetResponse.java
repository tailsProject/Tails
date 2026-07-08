package com.tails.pet.dto;

import com.tails.pet.Pet;

// MemberResponse에 반려동물 목록을 담아 내려주기 위한 최소 응답 DTO
public record PetResponse(Long petId, String name) {
    public static PetResponse from(Pet pet) {
        return new PetResponse(pet.getId(), pet.getName());
    }
}
