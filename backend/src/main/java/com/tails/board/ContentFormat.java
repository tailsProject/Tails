package com.tails.board;

// 게시글 본문 저장 형식 구분
// PLAIN: 순수 텍스트, 예전 글과 인라인 이미지 마커를 쓰던 과도기 글 포함
// HTML: 리치 텍스트 에디터로 작성한 글, 서식과 이미지가 HTML 태그로 저장됨
public enum ContentFormat {
    PLAIN,
    HTML
}
