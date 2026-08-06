package com.tails.traveldetail.dto;

import java.time.LocalTime;


// visitTime/memo는 null도 "값을 지운다"는 유효한 의도라 검증을 걸지 않음
public record TravelDetailUpdateRequest(LocalTime visitTime, String memo) {
}
