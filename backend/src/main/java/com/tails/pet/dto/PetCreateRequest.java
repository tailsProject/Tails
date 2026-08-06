package com.tails.pet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PetCreateRequest(

        @NotBlank(message = "이름을 입력해주세요.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name,

        @Size(max = 30, message = "종류는 30자 이하여야 합니다.")
        String species,

        @PastOrPresent(message = "생일은 오늘 이전이어야 합니다.")
        LocalDate birthDate
) {
}
