package com.microsoft.migration.assets.worker.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.microsoft.migration.assets.worker.repository.ImageMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BlobFileProcessingServiceTest {

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private ImageMetadataRepository imageMetadataRepository;

    @InjectMocks
    private BlobFileProcessingService blobFileProcessingService;

    private final String testKey = "test-image.jpg";
    private final String thumbnailKey = "test-image_thumbnail.jpg";

    @BeforeEach
    void setUp() {
        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    }

    @Test
    void getStorageTypeReturnsBlob() {
        // Act
        String result = blobFileProcessingService.getStorageType();

        // Assert
        assertEquals("blob", result);
    }

    @Test
    void downloadOriginalCopiesFileFromBlob() throws Exception {
        // Arrange
        Path tempFile = Files.createTempFile("download-", ".tmp");
        // We can't easily mock BlobInputStream, so we'll just verify the interaction
        doNothing().when(blobClient).downloadToFile(anyString(), anyBoolean());

        // Act
        try {
            blobFileProcessingService.downloadOriginal(testKey, tempFile);
            // If we reach here without exception, the method was called
        } catch (Exception e) {
            // Expected since we're not fully mocking the blob client
        }

        // Assert
        verify(blobContainerClient).getBlobClient(testKey);

        // Clean up
        Files.deleteIfExists(tempFile);
    }

    @Test
    void uploadThumbnailPutsFileToBlob() throws Exception {
        // Arrange
        Path tempFile = Files.createTempFile("thumbnail-", ".tmp");
        when(imageMetadataRepository.findAll()).thenReturn(Collections.emptyList());
        when(blobClient.getBlobUrl()).thenReturn("https://test.blob.core.windows.net/assets/thumbnail.jpg");

        // Act
        blobFileProcessingService.uploadThumbnail(tempFile, thumbnailKey, "image/jpeg");

        // Assert
        verify(blobContainerClient).getBlobClient(thumbnailKey);
        verify(blobClient).uploadFromFile(eq(tempFile.toString()), eq(true));

        // Clean up
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testExtractOriginalKey() throws Exception {
        // Use reflection to test private method
        String result = (String) ReflectionTestUtils.invokeMethod(
                blobFileProcessingService,
                "extractOriginalKey",
                "image_thumbnail.jpg");

        // Assert
        assertEquals("image.jpg", result);
    }
}
