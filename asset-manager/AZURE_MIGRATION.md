# Azure Migration Guide for Asset Manager

This guide explains the migration of the Asset Manager application from AWS services to Azure services.

## Migration Summary

The following migrations have been completed:

### 1. Storage Migration: AWS S3 → Azure Blob Storage

**Changes:**
- Replaced AWS SDK dependency with Azure Spring Cloud Storage Blob Starter
- Updated configuration from AWS credentials to Azure connection string
- Migrated `AwsS3Service` to `AzureBlobService`
- Renamed `S3StorageItem` to `BlobStorageItem`
- Updated `S3FileProcessingService` to `BlobFileProcessingService` in worker module

**Configuration:**
```properties
# Old (AWS S3)
aws.accessKey=your-access-key
aws.secretKey=your-secret-key
aws.region=us-east-1
aws.s3.bucket=your-bucket-name

# New (Azure Blob Storage)
azure.storage.connection-string=your-connection-string
azure.storage.container-name=assets
```

### 2. Messaging Migration: RabbitMQ → Azure Service Bus

**Changes:**
- Replaced Spring AMQP (RabbitMQ) dependency with Azure Spring Cloud Service Bus JMS Starter
- Updated configuration from RabbitMQ host/port to Service Bus connection string
- Migrated from `@RabbitListener` to `@JmsListener`
- Changed from `RabbitTemplate` to `JmsTemplate` for message sending
- Simplified message acknowledgment (JMS handles this automatically with client acknowledge mode)

**Configuration:**
```properties
# Old (RabbitMQ)
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# New (Azure Service Bus)
spring.jms.servicebus.connection-string=your-servicebus-connection-string
spring.jms.servicebus.pricing-tier=premium
spring.jms.listener.acknowledge-mode=client
```

### 3. Database: PostgreSQL (No Change)

The application continues to use PostgreSQL. For Azure deployment, you can use Azure Database for PostgreSQL.

**Configuration:**
```properties
spring.datasource.url=jdbc:postgresql://your-server.postgres.database.azure.com:5432/assets_manager
spring.datasource.username=your-username@your-server
spring.datasource.password=your-password
```

## Prerequisites for Azure Deployment

### 1. Azure Blob Storage

Create an Azure Storage Account:
```bash
az storage account create \
  --name yourstorageaccount \
  --resource-group your-resource-group \
  --location eastus \
  --sku Standard_LRS

# Get connection string
az storage account show-connection-string \
  --name yourstorageaccount \
  --resource-group your-resource-group
```

Create a container:
```bash
az storage container create \
  --name assets \
  --account-name yourstorageaccount \
  --connection-string "your-connection-string"
```

### 2. Azure Service Bus

Create an Azure Service Bus namespace (Premium tier required for JMS):
```bash
az servicebus namespace create \
  --name your-servicebus-namespace \
  --resource-group your-resource-group \
  --location eastus \
  --sku Premium

# Create a queue
az servicebus queue create \
  --name image-processing \
  --namespace-name your-servicebus-namespace \
  --resource-group your-resource-group

# Get connection string
az servicebus namespace authorization-rule keys list \
  --resource-group your-resource-group \
  --namespace-name your-servicebus-namespace \
  --name RootManageSharedAccessKey \
  --query primaryConnectionString \
  --output tsv
```

### 3. Azure Database for PostgreSQL

Create a PostgreSQL server:
```bash
az postgres server create \
  --name your-postgres-server \
  --resource-group your-resource-group \
  --location eastus \
  --admin-user adminuser \
  --admin-password YourPassword123 \
  --sku-name B_Gen5_1

# Create database
az postgres db create \
  --name assets_manager \
  --resource-group your-resource-group \
  --server-name your-postgres-server
```

## Local Development

For local development, use the `dev` profile which uses local file storage instead of Azure Blob Storage:

### Web Module
```bash
cd web
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Worker Module
```bash
cd worker
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The `dev` profile:
- Uses `LocalFileStorageService` for file storage
- Uses `LocalFileProcessingService` for thumbnail generation
- Stores files in `../storage` directory
- Still requires a JMS connection for messaging (use local Service Bus emulator or configure appropriately)

## Deployment to Azure

### Option 1: Azure Container Apps

1. Build Docker images:
```bash
# Web module
docker build -t asset-manager-web:latest -f web/Dockerfile .

# Worker module
docker build -t asset-manager-worker:latest -f worker/Dockerfile .
```

2. Push to Azure Container Registry:
```bash
az acr login --name yourregistry
docker tag asset-manager-web:latest yourregistry.azurecr.io/asset-manager-web:latest
docker tag asset-manager-worker:latest yourregistry.azurecr.io/asset-manager-worker:latest
docker push yourregistry.azurecr.io/asset-manager-web:latest
docker push yourregistry.azurecr.io/asset-manager-worker:latest
```

3. Deploy to Container Apps:
```bash
# Create environment
az containerapp env create \
  --name asset-manager-env \
  --resource-group your-resource-group \
  --location eastus

# Deploy web app
az containerapp create \
  --name asset-manager-web \
  --resource-group your-resource-group \
  --environment asset-manager-env \
  --image yourregistry.azurecr.io/asset-manager-web:latest \
  --env-vars \
    AZURE_STORAGE_CONNECTION_STRING=your-connection-string \
    AZURE_STORAGE_CONTAINER_NAME=assets \
    SPRING_JMS_SERVICEBUS_CONNECTION_STRING=your-servicebus-connection \
    SPRING_DATASOURCE_URL=your-postgres-url \
    SPRING_DATASOURCE_USERNAME=your-username \
    SPRING_DATASOURCE_PASSWORD=your-password

# Deploy worker app
az containerapp create \
  --name asset-manager-worker \
  --resource-group your-resource-group \
  --environment asset-manager-env \
  --image yourregistry.azurecr.io/asset-manager-worker:latest \
  --env-vars \
    AZURE_STORAGE_CONNECTION_STRING=your-connection-string \
    AZURE_STORAGE_CONTAINER_NAME=assets \
    SPRING_JMS_SERVICEBUS_CONNECTION_STRING=your-servicebus-connection \
    SPRING_DATASOURCE_URL=your-postgres-url \
    SPRING_DATASOURCE_USERNAME=your-username \
    SPRING_DATASOURCE_PASSWORD=your-password
```

### Option 2: Azure App Service

Deploy as Spring Boot JARs to Azure App Service:

```bash
# Package the applications
mvn clean package -DskipTests

# Create App Service plan
az appservice plan create \
  --name asset-manager-plan \
  --resource-group your-resource-group \
  --sku P1V2 \
  --is-linux

# Deploy web app
az webapp create \
  --name asset-manager-web \
  --resource-group your-resource-group \
  --plan asset-manager-plan \
  --runtime "JAVA:8-jre8"

az webapp config appsettings set \
  --name asset-manager-web \
  --resource-group your-resource-group \
  --settings \
    AZURE_STORAGE_CONNECTION_STRING=your-connection-string \
    AZURE_STORAGE_CONTAINER_NAME=assets \
    SPRING_JMS_SERVICEBUS_CONNECTION_STRING=your-servicebus-connection \
    SPRING_DATASOURCE_URL=your-postgres-url \
    SPRING_DATASOURCE_USERNAME=your-username \
    SPRING_DATASOURCE_PASSWORD=your-password

az webapp deploy \
  --name asset-manager-web \
  --resource-group your-resource-group \
  --src-path web/target/assets-manager-web-0.0.1-SNAPSHOT.jar \
  --type jar

# Repeat for worker app
```

## Key Differences from AWS Implementation

### Storage Operations

**Listing Blobs:**
- AWS: `s3Client.listObjectsV2(request)`
- Azure: `blobContainerClient.listBlobs()`

**Uploading:**
- AWS: `s3Client.putObject(request, RequestBody.fromInputStream(...))`
- Azure: `blobClient.upload(inputStream, size, overwrite)`

**Downloading:**
- AWS: `s3Client.getObject(request)`
- Azure: `blobClient.openInputStream()`

**Deleting:**
- AWS: `s3Client.deleteObject(request)`
- Azure: `blobClient.delete()`

### Messaging Operations

**Sending Messages:**
- RabbitMQ: `rabbitTemplate.convertAndSend(queue, message)`
- Service Bus: `jmsTemplate.convertAndSend(destination, message)`

**Receiving Messages:**
- RabbitMQ: `@RabbitListener(queues = "queue-name")` with manual Channel acknowledgment
- Service Bus: `@JmsListener(destination = "queue-name")` with automatic acknowledgment in client mode

### Error Handling

**RabbitMQ:**
- Manual acknowledgment with `channel.basicAck()` or `channel.basicNack()`
- Requires try-catch for acknowledgment exceptions

**Azure Service Bus:**
- Automatic acknowledgment in CLIENT mode
- Throw exception to trigger retry
- Simpler error handling

## Troubleshooting

### Issue: JMS Connection Fails

**Solution:** Ensure you're using Premium tier Service Bus. Standard tier doesn't support JMS.

### Issue: Blob Upload Fails

**Solution:** Check connection string and container permissions. Ensure the container exists.

### Issue: Messages Not Processing

**Solution:** Verify Service Bus queue exists and connection string is correct. Check application logs for JMS listener errors.

## Additional Resources

- [Azure Blob Storage Documentation](https://docs.microsoft.com/azure/storage/blobs/)
- [Azure Service Bus Documentation](https://docs.microsoft.com/azure/service-bus-messaging/)
- [Azure Database for PostgreSQL Documentation](https://docs.microsoft.com/azure/postgresql/)
- [Spring Cloud Azure Documentation](https://docs.microsoft.com/azure/developer/java/spring-framework/)
