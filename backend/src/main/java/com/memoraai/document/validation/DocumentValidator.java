package com.memoraai.document.validation;

import com.memoraai.common.exception.DocumentValidationException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Component
public class DocumentValidator {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "pdf", "docx", "pptx", "txt", "png", "jpg", "jpeg"
    );

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "image/png",
            "image/jpeg"
    );

    public void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DocumentValidationException("File is required and cannot be empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new DocumentValidationException("File name cannot be empty");
        }

        String extension = getExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new DocumentValidationException("File extension not allowed: " + extension);
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase())) {
            throw new DocumentValidationException("MIME type not allowed: " + mimeType);
        }
    }

    public String getExtension(String filename) {
        if (filename == null) return "";
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }
}
