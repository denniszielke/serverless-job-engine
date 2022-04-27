param location string = resourceGroup().location
param environmentName string = 'env-${resourceGroup().name}'
param internalOnly bool

module storage 'storage.bicep' = {
  name: 'container-app-storage'
  params: {
    storageAccountName: 'strg${resourceGroup().name}'
  }
}

module logging 'logging.bicep' = {
  name: 'container-app-logging'
  params: {
    logAnalyticsWorkspaceName: 'logs-${environmentName}'
    appInsightsName: 'appins-${environmentName}'
  }
}

module environment 'environment.bicep' = {
  name: 'container-app-environment'
  params: {
    environmentName: environmentName
    internalOnly: internalOnly
    logAnalyticsCustomerId: logging.outputs.logAnalyticsCustomerId
    logAnalyticsSharedKey: logging.outputs.logAnalyticsSharedKey
    appInsightsInstrumentationKey: logging.outputs.appInsightsInstrumentationKey
  }
}

// module appqueueworker 'app-queue-worker.bicep' = {
//   name: 'container-app-js-calc-frontend'
//   params: {
//     containerRegistryPath: 'ghcr.io/${containerRegistryOwner}/container-apps/optimizer:${imageTag}'
//     environmentName: environmentName
//     storageAccountName: storage.outputs.storageAccountName
//   }
// }


// az deployment group create -g dzca15cgithub -f ./deploy/apps.bicep -p explorerImageTag=latest -p calculatorImageTag=latest  -p containerRegistryOwner=denniszielke
