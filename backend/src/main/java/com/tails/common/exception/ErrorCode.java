package com.tails.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 서비스 전체에서 쓰는 에러 코드/HTTP 상태/메시지 모음
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    
    // ===== 공통 =====
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "이미 사용 중인 정보입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // ===== 회원(Member) 관련 =====
    PASSWORD_NOT_MATCHED(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다. 다시 로그인해주세요."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    WRONG_PASSWORD(HttpStatus.BAD_REQUEST, "기존 비밀번호가 올바르지 않습니다."),
    SAME_AS_OLD_PASSWORD(HttpStatus.BAD_REQUEST, "새 비밀번호가 기존 비밀번호와 같습니다."),

    // ===== 게시글(Board) 관련 =====
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    NOT_BOARD_OWNER(HttpStatus.FORBIDDEN, "본인 게시글만 수정/삭제할 수 있습니다."),

    // ===== 댓글(Comment) 관련 =====
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    PARENT_COMMENT_BOARD_MISMATCH(HttpStatus.BAD_REQUEST, "다른 게시글의 댓글에는 답글을 달 수 없습니다."),
    NOT_COMMENT_OWNER(HttpStatus.FORBIDDEN, "본인 댓글만 수정/삭제할 수 있습니다."),

    // ===== 여행 일정(Travel) 관련 =====
    TRAVEL_NOT_FOUND(HttpStatus.NOT_FOUND, "여행 일정을 찾을 수 없습니다."),
    NOT_TRAVEL_OWNER(HttpStatus.FORBIDDEN, "본인의 여행 일정에만 접근할 수 있습니다."),
    TRAVEL_DETAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "여행 일정 세부 정보를 찾을 수 없습니다."),
    // 종료일이 시작일보다 빠른 경우처럼, 필드 하나만으로는 판단할 수 없고 두 필드를 같이 봐야
    // 알 수 있는 규칙이라 Bean Validation(@NotNull 등) 대신 TravelService에서 직접 검증한다.
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "종료일은 시작일보다 빠를 수 없습니다."),

    // ===== 장소(Place) 관련 =====
    // PlaceService의 장소 상세 조회, TravelDetailService가 여행 일정에 장소를 추가할 때
    // (요청으로 받은 placeId 검증) 둘 다에서 재사용한다.
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다."),

    // ===== 리뷰(Review) 관련 =====
    // (place_id, member_id) UNIQUE — 한 회원은 한 장소에 리뷰 하나만
    DUPLICATE_REVIEW(HttpStatus.CONFLICT, "이미 이 장소에 리뷰를 작성했습니다.");

    private final HttpStatus status;
    private final String message;
}
