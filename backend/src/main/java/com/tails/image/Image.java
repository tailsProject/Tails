package com.tails.image;

import com.tails.board.Board;
import com.tails.review.Review;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// 게시글/후기 이미지 정보를 관리하는 엔티티
@Entity
@Table(name = "image", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"board_id", "sequence"}),
        @UniqueConstraint(columnNames = {"review_id", "sequence"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id;
    
    // 게시글 이미지인 경우에만 값이 저장됨
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Board board;

    // 후기 이미지인 경우에만 값이 저장됨
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Review review;

    // 서버에 저장되는 파일명
    @Column(name = "stored_file_name", nullable = false, length = 300)
    private String storedFileName;

    // 사용자가 업로드한 원본 파일명
    @Column(name = "original_file_name", nullable = false, length = 300)
    private String originalFileName;

    // 이미지 순서/대표 이미지 지정. 0이 대표 이미지
    @Column(nullable = false, columnDefinition = "INT NOT NULL DEFAULT 0")
    private int sequence;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Image(Board board, Review review, String storedFileName, String originalFileName, int sequence) {
        this.board = board;
        this.review = review;
        this.storedFileName = storedFileName;
        this.originalFileName = originalFileName;
        this.sequence = sequence;
    }

    public void changeSequence(int sequence) {
        this.sequence = sequence;
    }
}
