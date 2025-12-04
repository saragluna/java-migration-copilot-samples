package com.microsoft.migration.assets.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.microsoft.migration.assets.model.BlobStorageItem;
import com.microsoft.migration.assets.model.ImageMetadata;
import com.microsoft.migration.assets.model.ImageProcessingMessage;
import com.microsoft.migration.assets.repository.ImageMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.microsoft.migration.assets.config.ServiceBusConfig.IMAGE_PROCESSING_QUEUE;

@Service
@RequiredArgsConstructor
@Profile("!dev") // Active when not in dev profile
public class AzureBlobService implements StorageService {

    private final BlobContainerClient blobContainerClient;
    private final JmsTemplate jmsTemplate;
    private final ImageMetadataRepository imageMetadataRepository;

    @Override
    public List<BlobStorageItem> listObjects() {
        return blobContainerClient.listBlobs().stream()
                .map(blobItem -> {
                    String blobName = blobItem.getName();
                    OffsetDateTime lastModified = blobItem.getProperties().getLastModified();
                    
                    Instant uploadedAt = imageMetadataRepository.findAll().stream()
                            .filter(metadata -> metadata.getS3Key().equals(blobName))
                            .map(metadata -> metadata.getUploadedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                            .findFirst()
                            .orElse(lastModified.toInstant());

                    return new BlobStorageItem(
                            blobName,
                            extractFilename(blobName),
                            blobItem.getProperties().getContentLength(),
                            lastModified.toInstant(),
                            uploadedAt,
                            generateUrl(blobName)
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    public void uploadObject(MultipartFile file) throws IOException {
        String key = generateKey(file.getOriginalFilename());
        BlobClient blobClient = blobContainerClient.getBlobClient(key);
        
        blobClient.upload(file.getInputStream(), file.getSize(), true);

        // Send message to queue for thumbnail generation
        ImageProcessingMessage message = new ImageProcessingMessage(
            key,
            file.getContentType(),
            getStorageType(),
            file.getSize()
        );
        jmsTemplate.convertAndSend(IMAGE_PROCESSING_QUEUE, message);

        // Create and save metadata to database
        ImageMetadata metadata = new ImageMetadata();
        metadata.setId(UUID.randomUUID().toString());
        metadata.setFilename(file.getOriginalFilename());
        metadata.setContentType(file.getContentType());
        metadata.setSize(file.getSize());
        metadata.setS3Key(key);
        metadata.setS3Url(generateUrl(key));

        imageMetadataRepository.save(metadata);
    }

    @Override
    public InputStream getObject(String key) throws IOException {
        BlobClient blobClient = blobContainerClient.getBlobClient(key);
        return blobClient.openInputStream();
    }

    @Override
    public void deleteObject(String key) throws IOException {
        BlobClient blobClient = blobContainerClient.getBlobClient(key);
        blobClient.delete();

        try {
            // Try to delete thumbnail if it exists
            BlobClient thumbnailBlobClient = blobContainerClient.getBlobClient(getThumbnailKey(key));
            thumbnailBlobClient.deleteIfExists();
        } catch (Exception e) {
            // Ignore if thumbnail doesn't exist
        }

        // Delete metadata from database
        imageMetadataRepository.findAll().stream()
                .filter(metadata -> metadata.getS3Key().equals(key))
                .findFirst()
                .ifPresent(metadata -> imageMetadataRepository.delete(metadata));
    }

    @Override
    public String getStorageType() {
        return "blob";
    }

    private String extractFilename(String key) {
        // Extract filename from the blob name
        int lastSlashIndex = key.lastIndexOf('/');
        return lastSlashIndex >= 0 ? key.substring(lastSlashIndex + 1) : key;
    }

    private String generateKey(String filename) {
        return UUID.randomUUID().toString() + "-" + filename;
    }
}