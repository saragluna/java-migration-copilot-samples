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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AzureBlobFileProcessingServiceTest {

    @Mock
    private BlobServiceClient blobServiceClient;

    @Mock
    private BlobContainerClient containerClient;

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
        when(blobServiceClient.getBlobContainerClient(containerName)).thenReturn(containerClient);
    }

    @Test
    void getStorageTypeReturnsBlob() {
        String result = azureBlobFileProcessingService.getStorageType();
        assertEquals("blob", result);
    }

    @Test
    void downloadOriginalCopiesFileFromBlob() throws Exception {
        Path tempFile = Files.createTempFile("download-", ".tmp");
        when(containerClient.getBlobClient(testKey)).thenReturn(blobClient);
        
        InputStream mockInputStream = new ByteArrayInputStream("test content".getBytes());
        when(blobClient.openInputStream()).thenReturn(mockInputStream);

        azureBlobFileProcessingService.downloadOriginal(testKey, tempFile);

        verify(blobClient).openInputStream();

        Files.deleteIfExists(tempFile);
    }

    @Test
    void uploadThumbnailPutsFileToBlob() throws Exception {
        Path tempFile = Files.createTempFile("thumbnail-", ".tmp");
        when(containerClient.getBlobClient(thumbnailKey)).thenReturn(blobClient);
        when(imageMetadataRepository.findAll()).thenReturn(Collections.emptyList());

        azureBlobFileProcessingService.uploadThumbnail(tempFile, thumbnailKey, "image/jpeg");

        verify(blobClient).uploadFromFile(tempFile.toString(), true);

        Files.deleteIfExists(tempFile);
    }

    @Test
    void testExtractOriginalKey() throws Exception {
        String result = (String) ReflectionTestUtils.invokeMethod(
                azureBlobFileProcessingService,
                "extractOriginalKey",
                "image_thumbnail.jpg");

        assertEquals("image.jpg", result);
    }
}
