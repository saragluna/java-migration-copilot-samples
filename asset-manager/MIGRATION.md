# AWS S3 to Azure Blob Storage Migration

## Overview

This document describes the migration of the `asset-manager` application from AWS S3 to Azure Blob Storage.

## Changes Summary

### 1. Dependencies Updated

#### Web Module (`web/pom.xml`)
- **Removed**: `software.amazon.awssdk:s3` (version 2.25.13)
- **Added**: `com.azure:azure-storage-blob` (version 12.25.1)

#### Worker Module (`worker/pom.xml`)
- **Removed**: `software.amazon.awssdk:s3` (version 2.25.13)
- **Added**: `com.azure:azure-storage-blob` (version 12.25.1)
- **Added**: `org.mockito:mockito-inline` (for testing final Azure SDK classes)

### 2. Configuration Changes

#### Web Module
**Created**: `AzureBlobConfig.java`
```java
- Configures BlobServiceClient using connection string
- Replaces AwsS3Config.java
```

**Updated**: `application.properties`
```properties
# Old AWS S3 Configuration
aws.accessKey=your-access-key
aws.secretKey=your-secret-key
aws.region=us-east-1
aws.s3.bucket=your-bucket-name

# New Azure Blob Storage Configuration
azure.storage.connection-string=DefaultEndpointsProtocol=https;AccountName=your-account-name;AccountKey=your-account-key;EndpointSuffix=core.windows.net
azure.storage.container-name=your-container-name
```

#### Worker Module
**Created**: `AzureBlobConfig.java`
- Same configuration pattern as web module

**Updated**: `application.properties`
- Same property changes as web module

### 3. Service Layer Changes

#### Web Module

**Created**: `AzureBlobService.java` (replaces `AwsS3Service.java`)
- Uses `BlobServiceClient`, `BlobContainerClient`, and `BlobClient`
- Methods remain compatible with `StorageService` interface
- Storage type changed from "s3" to "blob"

Key API Changes:
- `S3Client.listObjectsV2()` → `BlobContainerClient.listBlobs()`
- `S3Client.putObject()` → `BlobClient.upload()`
- `S3Client.getObject()` → `BlobClient.openInputStream()`
- `S3Client.deleteObject()` → `BlobClient.delete()`

#### Worker Module

**Created**: `BlobFileProcessingService.java` (replaces `S3FileProcessingService.java`)
- Uses Azure Blob Storage SDK for file processing
- Maintains same thumbnail generation workflow
- URL generation now uses `BlobClient.getBlobUrl()`

### 4. Model Changes

**Created**: `BlobStorageItem.java` (replaces `S3StorageItem.java`)
- Same structure, renamed for consistency with Azure naming

**Updated**: `ImageMetadata.java` (both web and worker modules)
- Changed `s3Key` → `blobName`
- Changed `s3Url` → `blobUrl`
- Other fields remain unchanged

### 5. Interface Updates

**Updated**: `StorageService.java`
- Return type changed from `List<S3StorageItem>` to `List<BlobStorageItem>`
- Comment updated from "AWS S3, local file system" to "Azure Blob Storage, local file system"
- Storage type comment updated from "s3 or local" to "blob or local"

**Updated**: `LocalFileStorageService.java`
- Updated to return `BlobStorageItem` instead of `S3StorageItem`
- No functional changes to local storage behavior

### 6. Controller Updates

**Updated**: `S3Controller.java`
- Import changed from `S3StorageItem` to `BlobStorageItem`
- All method signatures updated to use `BlobStorageItem`
- No changes to endpoint mappings or HTTP methods

### 7. Test Updates

**Created**: `BlobFileProcessingServiceTest.java`
- New test suite for `BlobFileProcessingService`
- Uses `mockito-inline` to mock final Azure SDK classes
- Tests storage type, upload thumbnail, and key extraction
- Simplified download test due to Azure SDK specifics

**Removed**: `S3FileProcessingServiceTest.java`

## Migration Checklist

To migrate an existing deployment from AWS S3 to Azure Blob Storage:

1. **Create Azure Resources**
   - [ ] Create an Azure Storage Account
   - [ ] Create a Blob Container
   - [ ] Note the connection string from Azure Portal

2. **Update Configuration**
   - [ ] Update `azure.storage.connection-string` in `application.properties`
   - [ ] Update `azure.storage.container-name` in `application.properties`
   - [ ] Remove old AWS credentials

3. **Migrate Data** (if needed)
   - [ ] Use Azure Storage Explorer or AzCopy to migrate existing files
   - [ ] Migrate database records (update `s3Key` to `blobName`, `s3Url` to `blobUrl`)

4. **Deploy Updated Application**
   - [ ] Build: `mvn clean package`
   - [ ] Deploy web and worker modules
   - [ ] Verify connectivity to Azure Blob Storage

5. **Test Functionality**
   - [ ] Test file upload
   - [ ] Test file listing
   - [ ] Test file viewing
   - [ ] Test file deletion
   - [ ] Verify thumbnail generation

## Database Schema Changes

The `ImageMetadata` entity has been updated with new column names:

```sql
-- If migrating existing data, run these SQL commands:
ALTER TABLE image_metadata RENAME COLUMN s3_key TO blob_name;
ALTER TABLE image_metadata RENAME COLUMN s3_url TO blob_url;
```

**Note**: If using `spring.jpa.hibernate.ddl-auto=update`, Hibernate will attempt to create new columns. You may need to manually migrate data and drop old columns.

## Compatibility Notes

- The storage interface remains compatible, allowing for easy switching between storage providers
- The local file storage option remains available for development/testing (enabled with `dev` profile)
- All existing endpoints and functionality are preserved
- Message queue integration (RabbitMQ) remains unchanged

## Build and Test

```bash
# Build the project
cd asset-manager
mvn clean package

# Run tests
mvn test

# Run specific module
cd web    # or worker
mvn spring-boot:run
```

## Benefits of Azure Blob Storage

1. **Scalability**: Automatic scaling without capacity planning
2. **Cost**: Tiered storage options for optimizing costs
3. **Integration**: Native integration with Azure services
4. **Security**: Advanced security features and encryption
5. **Availability**: High availability and geo-redundancy options

## Support

For issues or questions, please refer to:
- [Azure Blob Storage Documentation](https://docs.microsoft.com/en-us/azure/storage/blobs/)
- [Azure SDK for Java Documentation](https://docs.microsoft.com/en-us/java/api/overview/azure/storage-blob-readme)
