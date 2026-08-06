package com.tails.common.util;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

// 이미지 파일 저장/삭제 처리
@Component
public class FileStorage {

    // 저장 확장자: 검증된 Content-Type 기준. 확장자 스푸핑 방지
    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp"
    );
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB

    private final Path uploadDir;

    public FileStorage(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    // 검증 후 저장하고 저장된 파일명 반환
    public String store(MultipartFile file) {
        validate(file);

        String storedFileName = UUID.randomUUID() + EXTENSION_BY_CONTENT_TYPE.get(file.getContentType());

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, uploadDir.resolve(storedFileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        return storedFileName;
    }

    // 이미 없으면 그냥 삭제
    public void delete(String storedFileName) {
        try {
            Files.deleteIfExists(uploadDir.resolve(storedFileName));
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    // 트랜잭션 커밋 후 삭제. 롤백해도 파일은 남게 처리
    public void deleteAfterCommit(String storedFileName) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    delete(storedFileName);
                }
            });
        } else {
            delete(storedFileName);
        }
    }

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }
        // null 체크 먼저
        if (file.getContentType() == null || !EXTENSION_BY_CONTENT_TYPE.containsKey(file.getContentType())) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(ErrorCode.FILE_TOO_LARGE);
        }
    }
}
