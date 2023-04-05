param environmentName string
param location string = resourceGroup().location
param logAnalyticsCustomerId string
param logAnalyticsSharedKey string
param appInsightsInstrumentationKey string
param internalOnly bool

resource vnet 'Microsoft.Network/virtualNetworks@2021-05-01' existing = {
  name: 'vnet-${resourceGroup().name}'
}

resource environment 'Microsoft.App/managedEnvironments@2022-11-01-preview' = {
  name: environmentName
  location: location
  sku: {
    name: 'Premium'
  }
  properties: {
    appLogsConfiguration: {
      destination: 'log-analytics'
      logAnalyticsConfiguration: {
        customerId: logAnalyticsCustomerId
        sharedKey: logAnalyticsSharedKey
      }
    }
    workloadProfiles: [
      {
        name: 'consumption'
        workloadProfileType: 'Consumption'
      }
      {
        name: 'f4-compute'
        workloadProfileType: 'F4'
        MinimumCount: 1
        MaximumCount: 3
      }
    ]
    daprAIInstrumentationKey: appInsightsInstrumentationKey
    vnetConfiguration: {
      infrastructureSubnetId: '${vnet.id}/subnets/aca-control'
      internal: internalOnly
    }
  }
}

output location string = location
output environmentId string = environment.id
