param location string = resourceGroup().location
param storageAccountName string = 'assets${uniqueString(resourceGroup().id)}' 
param blobContainerName string = 'output'
param checkpointContainerName string = 'checkpoints'

resource mediaStorageAccount 'Microsoft.Storage/storageAccounts@2021-06-01' = {
   name: storageAccountName
   location: location 
   sku: { 
     name: 'Standard_LRS' 
   } 
   kind: 'StorageV2' 
   properties: { 
     accessTier: 'Hot' 
   } 
}

resource outputContainer 'Microsoft.Storage/storageAccounts/blobServices/containers@2021-08-01' = {
  name: '${mediaStorageAccount.name}/default/${blobContainerName}'
}

resource checkpointContainer 'Microsoft.Storage/storageAccounts/blobServices/containers@2021-08-01' = {
  name: '${mediaStorageAccount.name}/default/${checkpointContainerName}'
}

output storageAccountName string = mediaStorageAccount.name
output storageAccountKey string = '${listKeys(mediaStorageAccount.id, mediaStorageAccount.apiVersion).keys[0].value}'
