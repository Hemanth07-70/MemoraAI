package com.memoraai.document.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    /**
     * Stores the file and returns the physical storage path.
     */
    String store(MultipartFile file, String uniqueFileName);

    /**
     * Loads the file as a Resource.
     */
    Resource loadAsResource(String storagePath);

    /**
     * Deletes the file.
     */
    void delete(String storagePath);
}
