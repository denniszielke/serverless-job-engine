param location string = resourceGroup().location
param environmentName string = '${resourceGroup().name}'
param internalOnly bool = false
param deployAKS bool = false
param deployACA bool = false

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

module servicebus 'servicebus.bicep' = {
  name: 'servicebus'
  params: {
    serviceBusName: 'sb${environmentName}'
  }
}

module logging 'logging.bicep' = {
  name: 'container-app-logging'
  params: {
    logAnalyticsWorkspaceName: 'logs-${environmentName}'
    appInsightsName: 'appins-${environmentName}'
  }
}

module vnet 'vnet.bicep' = {
  name: 'vnet'
  params: {
    location: location
  }
}

module environment 'environment.bicep' = if (deployACA) {
  name: 'container-app-environment'
  params: {
    environmentName: '${environmentName}'
    internalOnly: internalOnly
    logAnalyticsCustomerId: logging.outputs.logAnalyticsCustomerId
    logAnalyticsSharedKey: logging.outputs.logAnalyticsSharedKey
    appInsightsInstrumentationKey: logging.outputs.appInsightsInstrumentationKey
  }
}

module cluster 'aks.bicep' = if (deployAKS) {
  name: 'aks'
  params: {
    environmentName: 'aks-${environmentName}'
    workspaceResourceId: logging.outputs.workspaceResourceId
  }
}
