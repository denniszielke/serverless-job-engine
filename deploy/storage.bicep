param location string = resourceGroup().location
param storageAccountName string = 'assets${uniqueString(resourceGroup().id)}' 
param containerName string = 'output'
param queueName string = 'requests'

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
  name: '${mediaStorageAccount.name}/default/${containerName}'
}

resource requestQueue 'Microsoft.Storage/storageAccounts/queueServices/queues@2021-08-01' = {
  name: '${mediaStorageAccount.name}/default/${queueName}'
}

output storageAccountName string = mediaStorageAccount.name
output storageAccountKey string = '${listKeys(mediaStorageAccount.id, mediaStorageAccount.apiVersion).keys[0].value}'
