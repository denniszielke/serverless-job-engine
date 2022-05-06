param location string = resourceGroup().location
param environmentName string = 'env-${resourceGroup().name}'
param internalOnly bool

module storage 'storage.bicep' = {
  name: 'container-app-storage'
  params: {
    storageAccountName: 'strg${resourceGroup().name}'
  }
}

module eventhub 'eventhub.bicep' = {
  name: 'eventhub'
  params: {
    location: location
    eventHubNamespaceName: 'evhbns${resourceGroup().name}'
    eventHubName: 'requests'
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
