param location string = resourceGroup().location
param environmentName string = 'env-${resourceGroup().name}'
param imageTag string
param containerRegistryOwner string


module storage 'storage.bicep' = {
  name: 'container-app-storage'
  params: {
    storageAccountName: 'strg${resourceGroup().name}'
  }
}

module appqueueworker 'app-queue-worker.bicep' = {
  name: 'container-app-queue-worker'
  params: {
    containerRegistryPath: 'ghcr.io/${containerRegistryOwner}/container-apps/optimizer:${imageTag}'
    environmentName: environmentName
    storageAccountName: storage.outputs.storageAccountName
    storageAccountKey: storage.outputs.storageAccountKey
  }
}


// az deployment group create -g dzca15cgithub -f ./deploy/apps.bicep -p explorerImageTag=latest -p calculatorImageTag=latest  -p containerRegistryOwner=denniszielke
