package com.microsoft.migration.assets.worker.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.microsoft.migration.assets.worker.repository.ImageMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AzureBlobFileProcessingServiceTest {

    @Mock
    private BlobServiceClient blobServiceClient;

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private ImageMetadataRepository imageMetadataRepository;

    @InjectMocks
    private AzureBlobFileProcessingService azureBlobFileProcessingService;

    private final String containerName = "test-container";
    private final String testKey = "test-image.jpg";
    private final String thumbnailKey = "test-image_thumbnail.jpg";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(azureBlobFileProcessingService, "containerName", containerName);
        when(blobServiceClient.getBlobContainerClient(anyString())).thenReturn(blobContainerClient);
        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    }

    @Test
    void getStorageTypeReturnsBlob() {
        // Act
        String result = azureBlobFileProcessingService.getStorageType();

        // Assert
        assertEquals("blob", result);
    }

    @Test
    void downloadOriginalCopiesFileFromBlob() throws Exception {
        // Arrange
        Path tempFile = Files.createTempFile("download-", ".tmp");
        ByteArrayInputStream mockInputStream = new ByteArrayInputStream("test content".getBytes());
        
        when(blobClient.openInputStream()).thenReturn(mockInputStream);

        // Act
        azureBlobFileProcessingService.downloadOriginal(testKey, tempFile);

        // Assert
        verify(blobServiceClient).getBlobContainerClient(containerName);
        verify(blobContainerClient).getBlobClient(testKey);
        verify(blobClient).openInputStream();

        // Clean up
        Files.deleteIfExists(tempFile);
    }

    @Test
    void uploadThumbnailPutsFileToBlob() throws Exception {
        // Arrange
        Path tempFile = Files.createTempFile("thumbnail-", ".tmp");
        Files.write(tempFile, "test content".getBytes());
        
        when(imageMetadataRepository.findAll()).thenReturn(Collections.emptyList());
        when(blobClient.getBlobUrl()).thenReturn("https://test.blob.core.windows.net/container/blob");

        // Act
        azureBlobFileProcessingService.uploadThumbnail(tempFile, thumbnailKey, "image/jpeg");

        // Assert
        verify(blobServiceClient).getBlobContainerClient(containerName);
        verify(blobContainerClient).getBlobClient(thumbnailKey);
        verify(blobClient).uploadFromFile(tempFile.toString(), true);

        // Clean up
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testExtractOriginalKey() throws Exception {
        // Use reflection to test private method
        String result = (String) ReflectionTestUtils.invokeMethod(
                azureBlobFileProcessingService,
                "extractOriginalKey",
                "image_thumbnail.jpg");

        // Assert
        assertEquals("image.jpg", result);
    }
}
