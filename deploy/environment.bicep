param environmentName string
param location string = resourceGroup().location
param logAnalyticsCustomerId string
param logAnalyticsSharedKey string
param appInsightsInstrumentationKey string
param internalOnly bool

resource vnet 'Microsoft.Network/virtualNetworks@2021-05-01' = {
  name: 'vnet-${resourceGroup().name}'
  location: resourceGroup().location
  properties: {
    addressSpace: {
      addressPrefixes: [
        '10.0.0.0/19'
      ]
    }
    subnets: [
      {
        name: 'gateway'
        properties: {
          addressPrefix: '10.0.0.0/24'
        }
      }
      {
        name: 'jumpbox'
        properties: {
          addressPrefix: '10.0.1.0/24'
        }
      }
      {
        name: 'apim'
        properties: {
          addressPrefix: '10.0.2.0/24'
        }
      }
      {
        name: 'AzureFirewallSubnet'
        properties: {
          addressPrefix: '10.0.3.0/24'
        }
      }
      {
        name: 'aca-control'
        properties: {
          addressPrefix: '10.0.8.0/21'
        }
      }
      {
        name: 'aca-apps'
        properties: {
          addressPrefix: '10.0.16.0/21'
        }
      }
    ]
  }
}

resource environment 'Microsoft.App/managedEnvironments@2022-06-01-preview' = {
  name: environmentName
  location: location
  properties: {
    type: 'managed'
    internalLoadBalancerEnabled: internalOnly
    appLogsConfiguration: {
      destination: 'log-analytics'
      logAnalyticsConfiguration: {
        customerId: logAnalyticsCustomerId
        sharedKey: logAnalyticsSharedKey
      }
    }
    daprAIInstrumentationKey: appInsightsInstrumentationKey
    vnetConfiguration: {
      dockerBridgeCidr: '172.17.0.1/16'
      platformReservedCidr: '10.2.0.0/20'
      platformReservedDnsIP: '10.2.0.10'
      infrastructureSubnetId: '${vnet.id}/subnets/aca-control'
      // runtimeSubnetId: '${vnet.id}/subnets/aca-apps'
      internal: internalOnly
    }
  }
}

output location string = location
output environmentId string = environment.id
