# Migration Plan: AWS S3 to Azure Blob Storage

## Overview
This document outlines the comprehensive migration plan for the asset-manager project from AWS S3 to Azure Blob Storage using the knowledge base ID: s3-to-azure-blob-storage.

## Project Information
- **Project**: asset-manager (multi-module Maven project)
- **Language**: Java 8
- **Framework**: Spring Boot 2.7.18
- **Build Tool**: Maven
- **Modules**: 
  - web (handles file uploads and viewing)
  - worker (handles thumbnail generation)
- **Current Storage**: AWS S3
- **Target Storage**: Azure Blob Storage

## Current State Analysis

### Dependencies to Replace
1. **Web Module** (`web/pom.xml`):
   - AWS SDK S3: `software.amazon.awssdk:s3` version 2.25.13
   
2. **Worker Module** (`worker/pom.xml`):
   - AWS SDK S3: `software.amazon.awssdk:s3` version 2.25.13

### Java Files to Migrate

#### Web Module
1. **Configuration**:
   - `web/src/main/java/com/microsoft/migration/assets/config/AwsS3Config.java`
     - Replace S3Client with BlobServiceClient
     - Replace AWS credentials with Azure connection string/credentials
     - Remove AWS Region configuration

2. **Service Layer**:
   - `web/src/main/java/com/microsoft/migration/assets/service/AwsS3Service.java`
     - Replace S3Client operations with BlobServiceClient/BlobContainerClient
     - Replace ListObjectsV2Request with blob listing
     - Replace PutObjectRequest with blob upload
     - Replace GetObjectRequest with blob download
     - Replace DeleteObjectRequest with blob delete
     - Update URL generation logic

3. **Model Classes**:
   - `web/src/main/java/com/microsoft/migration/assets/model/S3StorageItem.java`
     - Rename to BlobStorageItem (or keep as StorageItem)
   - `web/src/main/java/com/microsoft/migration/assets/model/ImageMetadata.java`
     - Rename fields: s3Key -> blobName, s3Url -> blobUrl

4. **Controller**:
   - `web/src/main/java/com/microsoft/migration/assets/controller/S3Controller.java`
     - Rename to BlobController or StorageController
     - Update references to S3StorageItem

5. **Interface**:
   - `web/src/main/java/com/microsoft/migration/assets/service/StorageService.java`
     - Update return type from S3StorageItem

#### Worker Module
1. **Configuration**:
   - `worker/src/main/java/com/microsoft/migration/assets/worker/config/AwsS3Config.java`
     - Replace S3Client with BlobServiceClient
     - Replace AWS credentials with Azure connection string/credentials

2. **Service Layer**:
   - `worker/src/main/java/com/microsoft/migration/assets/worker/service/S3FileProcessingService.java`
     - Replace S3Client operations with BlobServiceClient/BlobContainerClient
     - Update downloadOriginal method
     - Update uploadThumbnail method
     - Update URL generation

3. **Model Classes**:
   - `worker/src/main/java/com/microsoft/migration/assets/worker/model/ImageMetadata.java`
     - Rename fields: s3Key -> blobName, s3Url -> blobUrl

4. **Tests**:
   - `worker/src/test/java/com/microsoft/migration/assets/worker/service/S3FileProcessingServiceTest.java`
     - Update test class name and mocking

### Configuration Files to Update

1. **Web Module** (`web/src/main/resources/application.properties`):
   - Replace AWS configuration:
     ```
     # Old
     aws.accessKey=your-access-key
     aws.secretKey=your-secret-key
     aws.region=us-east-1
     aws.s3.bucket=your-bucket-name
     
     # New
     azure.storage.account-name=your-account-name
     azure.storage.account-key=your-account-key
     azure.storage.blob-endpoint=https://<account-name>.blob.core.windows.net
     azure.storage.container-name=your-container-name
     ```

2. **Worker Module** (`worker/src/main/resources/application.properties`):
   - Similar AWS to Azure configuration replacement

## Migration Steps

### Phase 1: Pre-Migration Setup
1. ✅ Verify project language (Java)
2. ✅ Create progress tracking file
3. ✅ Generate migration plan
4. ⬜ Initialize version control
5. ⬜ Verify build environment (JDK, Maven)

### Phase 2: Dependency Migration
1. ⬜ Update web/pom.xml:
   - Remove: `software.amazon.awssdk:s3`
   - Add: `com.azure:azure-storage-blob` (latest stable version)

2. ⬜ Update worker/pom.xml:
   - Remove: `software.amazon.awssdk:s3`
   - Add: `com.azure:azure-storage-blob` (latest stable version)

### Phase 3: Configuration Migration
1. ⬜ Web Module - AwsS3Config.java:
   - Rename to AzureBlobConfig.java
   - Replace S3Client bean with BlobServiceClient bean
   - Update configuration properties from AWS to Azure

2. ⬜ Worker Module - AwsS3Config.java:
   - Rename to AzureBlobConfig.java
   - Replace S3Client bean with BlobServiceClient bean
   - Update configuration properties from AWS to Azure

### Phase 4: Service Layer Migration
1. ⬜ Web Module - AwsS3Service.java:
   - Rename to AzureBlobService.java
   - Replace S3Client with BlobServiceClient/BlobContainerClient
   - Update listObjects() method
   - Update uploadObject() method
   - Update getObject() method
   - Update deleteObject() method
   - Update URL generation logic
   - Update getStorageType() to return "blob"

2. ⬜ Worker Module - S3FileProcessingService.java:
   - Rename to BlobFileProcessingService.java
   - Replace S3Client with BlobServiceClient/BlobContainerClient
   - Update downloadOriginal() method
   - Update uploadThumbnail() method
   - Update generateUrl() method
   - Update getStorageType() to return "blob"

### Phase 5: Model and Interface Updates
1. ⬜ Web Module - S3StorageItem.java:
   - Rename to BlobStorageItem.java (or StorageItem.java)

2. ⬜ Web Module - ImageMetadata.java:
   - Rename s3Key field to blobName
   - Rename s3Url field to blobUrl

3. ⬜ Worker Module - ImageMetadata.java:
   - Rename s3Key field to blobName
   - Rename s3Url field to blobUrl

4. ⬜ Web Module - StorageService.java:
   - Update return type references

5. ⬜ Web Module - S3Controller.java:
   - Rename to BlobController.java or StorageController.java
   - Update all references

### Phase 6: Configuration Files
1. ⬜ Update web/src/main/resources/application.properties
2. ⬜ Update worker/src/main/resources/application.properties

### Phase 7: Test Updates
1. ⬜ Update S3FileProcessingServiceTest.java
   - Rename to BlobFileProcessingServiceTest.java
   - Update mocking and assertions

### Phase 8: Validation and Fixing (Iterative)
1. ⬜ Build project with Maven
2. ⬜ Fix compilation errors
3. ⬜ Run unit tests
4. ⬜ CVE validation and fixing
5. ⬜ Consistency validation
6. ⬜ Completeness validation
7. ⬜ Repeat until all validations pass

### Phase 9: Final Summary
1. ⬜ Final code commit
2. ⬜ Generate migration summary report

## Expected Changes Summary

### Files to be Created
- `web/src/main/java/com/microsoft/migration/assets/config/AzureBlobConfig.java`
- `web/src/main/java/com/microsoft/migration/assets/service/AzureBlobService.java`
- `web/src/main/java/com/microsoft/migration/assets/model/BlobStorageItem.java`
- `web/src/main/java/com/microsoft/migration/assets/controller/BlobController.java`
- `worker/src/main/java/com/microsoft/migration/assets/worker/config/AzureBlobConfig.java`
- `worker/src/main/java/com/microsoft/migration/assets/worker/service/BlobFileProcessingService.java`
- `worker/src/test/java/com/microsoft/migration/assets/worker/service/BlobFileProcessingServiceTest.java`

### Files to be Deleted (or Renamed)
- All old AWS S3 related files will be replaced

### Files to be Modified
- Both pom.xml files (dependencies)
- Both application.properties files (configuration)
- Both ImageMetadata.java files (field names)
- ImageProcessingMessage.java files (if needed)
- StorageService.java interface
- LocalFileStorageService.java (for field name consistency)

## Key Azure Blob Storage Concepts

### Azure SDK Equivalents
| AWS S3 | Azure Blob Storage |
|--------|-------------------|
| S3Client | BlobServiceClient |
| Bucket | Container |
| Object/Key | Blob/Blob Name |
| PutObjectRequest | BlobClient.upload() |
| GetObjectRequest | BlobClient.openInputStream() |
| DeleteObjectRequest | BlobClient.delete() |
| ListObjectsV2Request | BlobContainerClient.listBlobs() |

### Authentication
- AWS: Access Key + Secret Key + Region
- Azure: Storage Account Name + Account Key OR Connection String OR Managed Identity

### URL Structure
- AWS S3: `https://bucket.s3.region.amazonaws.com/key`
- Azure Blob: `https://accountname.blob.core.windows.net/container/blobname`

## Risk Mitigation
1. Keep LocalFileStorageService unchanged for development/testing
2. Maintain StorageService interface to minimize controller changes
3. Update field names incrementally
4. Test thoroughly after each phase
5. Use version control to track all changes

## Success Criteria
- ✅ All code compiles without errors
- ✅ All unit tests pass
- ✅ No new CVEs introduced
- ✅ Code maintains functional consistency
- ✅ All AWS dependencies removed
- ✅ All Azure dependencies added
- ✅ Configuration files updated correctly
- ✅ All S3 references replaced with Blob Storage equivalents
