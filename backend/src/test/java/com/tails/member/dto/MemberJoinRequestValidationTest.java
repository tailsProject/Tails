package com.tails.member.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// MemberJoinRequest Bean Validation 단위테스트 - email @Size(max=100) 포함
class MemberJoinRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private MemberJoinRequest requestWithEmail(String email) {
        return new MemberJoinRequest(email, "Test1234!", "Test1234!", "tester", null);
    }

    @Test
    void 이메일이_100자를_넘으면_검증에_걸린다() {
        String tooLong = "a".repeat(96) + "@test.com"; // 105자
        Set<ConstraintViolation<MemberJoinRequest>> violations = validator.validate(requestWithEmail(tooLong));

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void 이메일이_100자_이하이면_길이_검증은_통과한다() {
        Set<ConstraintViolation<MemberJoinRequest>> violations = validator.validate(requestWithEmail("normal@test.com"));

        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("email"));
    }
}
