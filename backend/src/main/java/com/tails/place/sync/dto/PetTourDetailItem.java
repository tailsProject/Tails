package com.tails.place.sync.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// detailPetTour2 응답의 반려동물 동반 상세정보. 값 없는 필드는 null 대신 빈 문자열로 내려오는 경우가 많음

@JsonIgnoreProperties(ignoreUnknown = true)
public record PetTourDetailItem(
        String contentid,
        String relaAcdntRiskMtr,
        String acmpyTypeCd,
        String relaPosesFclty,
        String relaFrnshPrdlst,
        String etcAcmpyInfo,
        String relaPurcPrdlst,
        String acmpyPsblCpam,
        String relaRntlPrdlst,
        String acmpyNeedMtr) {
}
