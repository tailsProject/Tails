package com.tails.pet;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.common.util.FileStorage;
import com.tails.member.MemberRepository;
import com.tails.pet.dto.PetCreateRequest;
import com.tails.pet.dto.PetResponse;
import com.tails.pet.dto.PetUpdateRequest;
import com.tails.travel.TravelRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

// 반려동물 CRUD 비즈니스 로직
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {

    private static final String UPLOAD_URL_PREFIX = "/uploads/";

    private final PetRepository petRepository;
    private final MemberRepository memberRepository;
    private final TravelRepository travelRepository;
    private final FileStorage fileStorage;

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
        deleteStoredPhotoIfExists(pet);
        // travel_pet은 cascade 없는 단순 참조라, 여행에 추가된 반려동물을 그냥 지우면 FK 위반이 남
        travelRepository.removePetFromAllTravels(petId);
        petRepository.delete(pet);
    }

    // 기존 사진이 우리 서버 파일이면 교체 시 함께 정리
    @Transactional
    public String uploadPhoto(Long memberId, Long petId, MultipartFile file) {
        Pet pet = getPetOrThrow(petId);
        requireOwner(pet, memberId);

        String storedFileName = fileStorage.store(file);
        deleteStoredPhotoIfExists(pet);
        pet.changePhotoImg(UPLOAD_URL_PREFIX + storedFileName);
        return pet.getPhotoImg();
    }

    @Transactional
    public void deletePhoto(Long memberId, Long petId) {
        Pet pet = getPetOrThrow(petId);
        requireOwner(pet, memberId);
        deleteStoredPhotoIfExists(pet);
        pet.changePhotoImg(null);
    }

    private void deleteStoredPhotoIfExists(Pet pet) {
        String currentUrl = pet.getPhotoImg();
        if (currentUrl != null && currentUrl.startsWith(UPLOAD_URL_PREFIX)) {
            fileStorage.deleteAfterCommit(currentUrl.substring(UPLOAD_URL_PREFIX.length()));
        }
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
