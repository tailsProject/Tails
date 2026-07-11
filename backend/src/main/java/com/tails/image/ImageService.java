package com.tails.image;

import com.tails.board.Board;
import com.tails.board.BoardRepository;
import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.common.util.FileStorage;
import com.tails.image.dto.ImageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

// 게시글 이미지 업로드/조회/삭제 비즈니스 로직
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageService {

    private final ImageRepository imageRepository;
    private final BoardRepository boardRepository;
    private final FileStorage fileStorage;

    // 게시글 작성자 본인만 업로드 가능, 여러 장 동시 업로드
    @Transactional
    public List<ImageResponse> uploadForBoard(Long memberId, Long boardId, List<MultipartFile> files) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
        if (board.getMember() == null || !board.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.NOT_IMAGE_OWNER);
        }

        // 파일 하나씩 검증하며 바로 저장하면, 뒤쪽 파일이 실패했을 때 이미 저장된 앞쪽 파일이
        // 고아 파일로 남는다 - 그래서 저장 도중 예외가 나면 이번에 저장한 파일들을 지우고 다시 던진다.
        List<String> storedFileNames = new ArrayList<>();
        try {
            return files.stream()
                    .map(file -> {
                        String storedFileName = fileStorage.store(file);
                        storedFileNames.add(storedFileName);
                        Image image = Image.builder()
                                .board(board)
                                .storedFileName(storedFileName)
                                .originalFileName(file.getOriginalFilename())
                                .build();
                        return ImageResponse.from(imageRepository.save(image));
                    })
                    .toList();
        } catch (RuntimeException e) {
            storedFileNames.forEach(fileStorage::delete);
            throw e;
        }
    }

    public List<ImageResponse> getByBoard(Long boardId) {
        return imageRepository.findByBoardIdOrderByCreatedAtAsc(boardId).stream()
                .map(ImageResponse::from)
                .toList();
    }

    // 게시글 작성자 본인만 삭제 가능
    @Transactional
    public void delete(Long memberId, Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new CustomException(ErrorCode.IMAGE_NOT_FOUND));

        Long ownerId = image.getBoard().getMember() != null ? image.getBoard().getMember().getId() : null;
        if (ownerId == null || !ownerId.equals(memberId)) {
            throw new CustomException(ErrorCode.NOT_IMAGE_OWNER);
        }

        imageRepository.delete(image);
        fileStorage.delete(image.getStoredFileName());
    }
}
