package com.tails.image;

import com.tails.board.Board;
import com.tails.board.BoardRepository;
import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.common.util.FileStorage;
import com.tails.image.dto.ImageResponse;
import com.tails.review.Review;
import com.tails.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

// 게시글/리뷰 이미지 업로드, 조회, 삭제 비즈니스 로직
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageService {

    private static final int MAX_FILE_COUNT = 10;

    private final ImageRepository imageRepository;
    private final BoardRepository boardRepository;
    private final ReviewRepository reviewRepository;
    private final FileStorage fileStorage;

    // 게시글 작성자 본인만 업로드 가능, 여러 장 동시 업로드
    @Transactional
    public List<ImageResponse> uploadForBoard(Long memberId, Long boardId, List<MultipartFile> files) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
        if (board.getMember() == null || !board.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.NOT_IMAGE_OWNER);
        }

        AtomicInteger sequence = new AtomicInteger(imageRepository.findMaxSequenceByBoardId(boardId) + 1);
        return storeAndSave(files, (storedFileName, originalFileName) -> Image.builder()
                .board(board)
                .storedFileName(storedFileName)
                .originalFileName(originalFileName)
                .sequence(sequence.getAndIncrement())
                .build());
    }

    // 리뷰 작성자 본인만 업로드 가능.
    @Transactional
    public List<ImageResponse> uploadForReview(Long memberId, Long reviewId, List<MultipartFile> files) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        if (review.getMember() == null || !review.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.NOT_IMAGE_OWNER);
        }

        AtomicInteger sequence = new AtomicInteger(imageRepository.findMaxSequenceByReviewId(reviewId) + 1);
        return storeAndSave(files, (storedFileName, originalFileName) -> Image.builder()
                .review(review)
                .storedFileName(storedFileName)
                .originalFileName(originalFileName)
                .sequence(sequence.getAndIncrement())
                .build());
    }

    // 업로드 중 예외가 발생하면 저장된 파일을 모두 삭제
    private List<ImageResponse> storeAndSave(List<MultipartFile> files,
                                              BiFunction<String, String, Image> imageFactory) {
        if (files == null || files.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }
        if (files.size() > MAX_FILE_COUNT) {
            throw new CustomException(ErrorCode.TOO_MANY_FILES);
        }
        // 저장 시작 전에 전부 미리 검증 - 뒤쪽 파일이 무효해도 앞쪽 파일이 디스크에 썼다가 지워지는 걸 방지
        files.forEach(fileStorage::validate);

        List<String> storedFileNames = new ArrayList<>();
        try {
            return files.stream()
                    .map(file -> {
                        String storedFileName = fileStorage.store(file);
                        storedFileNames.add(storedFileName);
                        Image image = imageFactory.apply(storedFileName, file.getOriginalFilename());
                        return ImageResponse.from(imageRepository.save(image));
                    })
                    .toList();
        } catch (RuntimeException e) {
            storedFileNames.forEach(fileStorage::delete);
            throw e;
        }
    }
    // 게시글 이미지 조회 - sequence 기준(대표 이미지가 항상 먼저 보임)
    public List<ImageResponse> getByBoard(Long boardId, Long currentMemberId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
        if (!board.isVisibleTo(currentMemberId)) {
            throw new CustomException(ErrorCode.BOARD_NOT_FOUND);
        }
        return imageRepository.findByBoardIdOrderBySequenceAsc(boardId).stream()
                .map(ImageResponse::from)
                .toList();
    }
    // 리뷰 이미지 조회
    public List<ImageResponse> getByReview(Long reviewId) {
        return imageRepository.findByReview_ReviewIdOrderByCreatedAtAsc(reviewId).stream()
                .map(ImageResponse::from)
                .toList();
    }

    // 게시글/리뷰 작성자 본인만 삭제 가능
    @Transactional
    public void delete(Long memberId, Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new CustomException(ErrorCode.IMAGE_NOT_FOUND));

        Long ownerId = resolveOwnerId(image);
        if (ownerId == null || !ownerId.equals(memberId)) {
            throw new CustomException(ErrorCode.NOT_IMAGE_OWNER);
        }

        imageRepository.delete(image);
        fileStorage.deleteAfterCommit(image.getStoredFileName());
    }

    // 게시글 작성자 본인만 순서 변경 가능. imageIds는 전체 이미지를 원하는 순서로 나열한 목록
    @Transactional
    public void reorderBoardImages(Long memberId, Long boardId, List<Long> imageIds) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
        if (board.getMember() == null || !board.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.NOT_IMAGE_OWNER);
        }

        List<Image> currentImages = imageRepository.findByBoardIdOrderBySequenceAsc(boardId);
        if (currentImages.size() != imageIds.size()) {
            throw new CustomException(ErrorCode.IMAGE_ORDER_MISMATCH);
        }

        Map<Long, Image> imageById = new LinkedHashMap<>();
        currentImages.forEach(image -> imageById.put(image.getId(), image));
        Set<Long> seenIds = new HashSet<>();
        for (Long imageId : imageIds) {
            if (!imageById.containsKey(imageId) || !seenIds.add(imageId)) {
                throw new CustomException(ErrorCode.IMAGE_ORDER_MISMATCH);
            }
        }

        // 1단계: (board_id, sequence) 유니크 제약 충돌을 피하기 위해 전부 음수 임시값으로 옮기고 flush
        int temp = -1;
        for (Image image : currentImages) {
            image.changeSequence(temp--);
        }
        imageRepository.flush();

        // 2단계: 요청받은 순서대로 최종 sequence(0부터) 부여
        for (int i = 0; i < imageIds.size(); i++) {
            imageById.get(imageIds.get(i)).changeSequence(i);
        }
    }

    // 이미지 소유자의 회원 id 조회. board/review 둘 다 null이면 불변식 위반이라 명확한 예외로 처리
    private Long resolveOwnerId(Image image) {
        if (image.getBoard() != null) {
            return image.getBoard().getMember() != null ? image.getBoard().getMember().getId() : null;
        }
        if (image.getReview() != null) {
            return image.getReview().getMember() != null ? image.getReview().getMember().getId() : null;
        }
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
