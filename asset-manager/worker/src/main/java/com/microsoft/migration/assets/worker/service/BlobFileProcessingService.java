package com.microsoft.migration.assets.worker.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.microsoft.migration.assets.worker.repository.ImageMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
@Profile("!dev")
@RequiredArgsConstructor
public class BlobFileProcessingService extends AbstractFileProcessingService {
    private final BlobContainerClient blobContainerClient;
    private final ImageMetadataRepository imageMetadataRepository;

    @Override
    public void downloadOriginal(String key, Path destination) throws Exception {
        BlobClient blobClient = blobContainerClient.getBlobClient(key);
                
        try (InputStream inputStream = blobClient.openInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void uploadThumbnail(Path source, String key, String contentType) throws Exception {
        BlobClient blobClient = blobContainerClient.getBlobClient(key);
                
        blobClient.uploadFromFile(source.toString(), true);

        // Extract the original key from the thumbnail key
        String originalKey = extractOriginalKey(key);
        
        // Find and update metadata
        imageMetadataRepository.findAll().stream()
            .filter(metadata -> metadata.getS3Key().equals(originalKey))
            .findFirst()
            .ifPresent(metadata -> {
                metadata.setThumbnailKey(key);
                metadata.setThumbnailUrl(generateUrl(key));
                imageMetadataRepository.save(metadata);
            });
    }

    @Override
    public String getStorageType() {
        return "blob";
    }

    @Override
    protected String generateUrl(String key) {
        BlobClient blobClient = blobContainerClient.getBlobClient(key);
        return blobClient.getBlobUrl();
    }

    private String extractOriginalKey(String key) {
        // For a key like "xxxxx_thumbnail.png", get "xxxxx.png"
        String suffix = "_thumbnail";
        int extensionIndex = key.lastIndexOf('.');
        if (extensionIndex > 0) {
            String nameWithoutExtension = key.substring(0, extensionIndex);
            String extension = key.substring(extensionIndex);
            
            int suffixIndex = nameWithoutExtension.lastIndexOf(suffix);
            if (suffixIndex > 0) {
                return nameWithoutExtension.substring(0, suffixIndex) + extension;
            }
        }
        return key;
    }
}