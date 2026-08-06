package com.tails.common.util;

// 위도/경도 좌표 계산 유틸. Place 반경 검색 등에서 공용으로 사용
// 상태 없는 순수 계산 함수만 있어 인스턴스화하지 않고 static으로 제공
public final class GeoUtil {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;
    // 위도 1도의 대략적인 거리(m). 경계 상자(bounding box) 폭 계산용 근사값
    private static final double METERS_PER_DEGREE_LAT = 111_320.0;

    private GeoUtil() {
    }

    // 두 좌표 사이의 거리(m)를 Haversine 공식으로 계산
    public static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    // 중심 좌표에서 radius(m) 반경을 감싸는 경계 상자의 위도 폭(도 단위)
    // DB에서 BETWEEN으로 1차 필터링한 뒤, 정확한 거리 계산(distanceMeters)으로 2차 필터링하기 위함
    public static double latDelta(double radiusMeters) {
        return radiusMeters / METERS_PER_DEGREE_LAT;
    }

    // 경계 상자의 경도 폭(도 단위). 경도 1도의 실제 거리는 위도에 따라 달라지므로 centerLat로 보정
    public static double lngDelta(double radiusMeters, double centerLat) {
        return radiusMeters / (METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(centerLat)));
    }
}
