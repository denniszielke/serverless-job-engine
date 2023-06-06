param location string = resourceGroup().location
param environmentName string = '${resourceGroup().name}'
param imageTag string
param containerRegistryOwner string

// module storage 'storage.bicep' = {
//   name: 'container-app-storage'
//   params: {
//     storageAccountName: 'strg${resourceGroup().name}'
//   }
// }

// module eventhub 'eventhub.bicep' = {
//   name: 'eventhub'
//   params: {
//     location: location
//     eventHubNamespaceName: 'evhbns${resourceGroup().name}'
//     eventHubName: 'requests'
//   }
// }

// module appqueueworker 'app-queue-worker.bicep' = {
//   name: 'container-app-queue-worker'
//   params: {
//     containerRegistryPath: 'ghcr.io/${containerRegistryOwner}/serverless-job-engine/engine:${imageTag}'
//     environmentName: environmentName
//     storageAccountName: storage.outputs.storageAccountName
//     storageAccountKey: storage.outputs.storageAccountKey
//     eventHubConnectionString: eventhub.outputs.eventHubNamespaceConnectionString
//     eventHubName: eventhub.outputs.eventHubName
//   }
// }

// module jobqueueworker 'job-queue-worker.bicep' = {
//   name: 'container-job-queue-worker'
//   params: {
//     containerRegistryPath: 'ghcr.io/${containerRegistryOwner}/serverless-job-engine/engine:${imageTag}'
//     environmentName: environmentName
//     storageAccountName: storage.outputs.storageAccountName
//     storageAccountKey: storage.outputs.storageAccountKey
//     eventHubConnectionString: eventhub.outputs.eventHubNamespaceConnectionString
//     eventHubName: eventhub.outputs.eventHubName
//   }
// }

module jobechoworker 'job-echo-worker.bicep' = {
  name: 'container-job-echo-worker'
  params: {
    environmentName: environmentName
  }
}

