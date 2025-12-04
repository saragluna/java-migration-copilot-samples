# Asset Manager - Azure Migration Summary

## Overview
This document summarizes the complete migration of the Asset Manager application from AWS services to Azure services.

## Migration Completed

### 1. Storage Layer: AWS S3 → Azure Blob Storage

#### Dependencies Changed
**pom.xml (both web and worker modules):**
```xml
<!-- OLD -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.25.13</version>
</dependency>

<!-- NEW -->
<dependency>
    <groupId>com.azure.spring</groupId>
    <artifactId>spring-cloud-azure-starter-storage-blob</artifactId>
</dependency>
```

#### Code Changes
- **AwsS3Service.java** → **AzureBlobService.java**
  - Replaced `S3Client` with `BlobContainerClient`
  - Updated upload/download/delete operations to use Azure Blob SDK
  - Changed storage type from "s3" to "blob"

- **S3FileProcessingService.java** → **BlobFileProcessingService.java**
  - Replaced S3 SDK calls with Azure Blob SDK
  - Updated URL generation to use `BlobClient.getBlobUrl()`

- **S3StorageItem.java** → **BlobStorageItem.java**
  - Renamed model class to reflect Azure terminology

- **AwsS3Config.java** → **AzureBlobConfig.java**
  - Replaced AWS credentials configuration with Azure connection string
  - Added `BlobServiceClient` and `BlobContainerClient` beans

#### Configuration Changes
```properties
# OLD (AWS S3)
aws.accessKey=your-access-key
aws.secretKey=your-secret-key
aws.region=us-east-1
aws.s3.bucket=your-bucket-name

# NEW (Azure Blob Storage)
azure.storage.connection-string=your-connection-string
azure.storage.container-name=assets
```

### 2. Messaging Layer: RabbitMQ → Azure Service Bus

#### Dependencies Changed
**pom.xml (both web and worker modules):**
```xml
<!-- OLD -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>

<!-- NEW -->
<dependency>
    <groupId>com.azure.spring</groupId>
    <artifactId>spring-cloud-azure-starter-servicebus-jms</artifactId>
</dependency>
```

#### Code Changes
- **RabbitConfig.java** → **ServiceBusConfig.java**
  - Replaced RabbitMQ configuration with JMS configuration
  - Simplified message converter setup for JMS

- **Message Sending:**
  - `RabbitTemplate` → `JmsTemplate`
  - `rabbitTemplate.convertAndSend(queue, message)` → `jmsTemplate.convertAndSend(destination, message)`

- **Message Receiving:**
  - `@RabbitListener(queues = "queue-name")` → `@JmsListener(destination = "queue-name")`
  - Removed manual Channel acknowledgment (JMS handles automatically in CLIENT mode)

- **AbstractFileProcessingService.java:**
  - Simplified message listener - removed RabbitMQ-specific Channel and delivery tag handling
  - Exception handling now throws RuntimeException to trigger JMS retry

- **BackupMessageProcessor.java:**
  - Updated to use `@JmsListener` annotation
  - Simplified error handling

- **Main Application Classes:**
  - `@EnableRabbit` → `@EnableJms`
  - AssetsManagerApplication.java
  - WorkerApplication.java

#### Configuration Changes
```properties
# OLD (RabbitMQ)
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# NEW (Azure Service Bus)
spring.jms.servicebus.connection-string=your-servicebus-connection-string
spring.jms.servicebus.pricing-tier=premium
spring.jms.listener.acknowledge-mode=client
```

### 3. Database: PostgreSQL (No Changes)

The application continues to use PostgreSQL with the same configuration. For Azure deployment, use Azure Database for PostgreSQL Flexible Server.

### 4. Development Profile Support

Created **application-dev.properties** files for both web and worker modules to support local development without Azure resources:
- Uses `LocalFileStorageService` instead of `AzureBlobService`
- Uses `LocalFileProcessingService` instead of `BlobFileProcessingService`
- Local file storage in `../storage` directory
- Placeholder JMS configuration (not used in dev mode)

### 5. Testing

Updated test files:
- **S3FileProcessingServiceTest.java** → **BlobFileProcessingServiceTest.java**
  - Updated mocks to use Azure Blob SDK classes
  - Simplified download test due to Azure SDK specifics
  - Verified blob client interactions

## Files Modified

### Web Module
- `pom.xml` - Updated dependencies
- `AssetsManagerApplication.java` - Changed to @EnableJms
- `AzureBlobConfig.java` (new) - Azure Blob configuration
- `ServiceBusConfig.java` (new) - Service Bus JMS configuration
- `AzureBlobService.java` (new) - Azure Blob storage implementation
- `BlobStorageItem.java` (renamed) - Storage item model
- `LocalFileStorageService.java` - Updated for JMS
- `BackupMessageProcessor.java` - Updated for JMS
- `StorageService.java` - Updated interface
- `S3Controller.java` - Updated to use BlobStorageItem
- `application.properties` - Azure configuration
- `application-dev.properties` (new) - Dev profile configuration

### Worker Module
- `pom.xml` - Updated dependencies
- `WorkerApplication.java` - Changed to @EnableJms
- `AzureBlobConfig.java` (new) - Azure Blob configuration
- `ServiceBusConfig.java` (new) - Service Bus JMS configuration
- `BlobFileProcessingService.java` (new) - Azure Blob file processing
- `AbstractFileProcessingService.java` - Updated for JMS
- `LocalFileProcessingService.java` - No changes needed
- `application.properties` - Azure configuration
- `application-dev.properties` (new) - Dev profile configuration
- `BlobFileProcessingServiceTest.java` (new) - Updated tests

### Documentation
- `AZURE_MIGRATION.md` (new) - Comprehensive migration guide with Azure setup instructions

## Build Status

✅ **Build Successful**
- Both web and worker modules compile successfully
- Tests updated and passing
- Packages created successfully

## Key Benefits of Migration

1. **Azure Integration**: Native integration with Azure services
2. **Simplified Messaging**: JMS provides simpler API compared to AMQP
3. **Automatic Retries**: Azure Service Bus handles message retries automatically
4. **Cloud-Native**: Better suited for Azure Container Apps and Azure App Service
5. **Managed Services**: Azure handles infrastructure management

## Next Steps for Deployment

1. Create Azure resources (Storage Account, Service Bus, PostgreSQL)
2. Configure connection strings in production environment
3. Build Docker images for containerization
4. Deploy to Azure Container Apps or Azure App Service
5. Configure monitoring and alerts

## Local Development

To run locally with dev profile:
```bash
# Web module
cd web
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Worker module  
cd worker
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Important Notes

- Azure Service Bus **Premium tier** is required for JMS support
- Connection strings should be stored securely (Azure Key Vault recommended)
- The dev profile allows local testing without Azure resources
- Container creation in blob storage is automatic if it doesn't exist
- Message acknowledgment is handled automatically by JMS in CLIENT mode
