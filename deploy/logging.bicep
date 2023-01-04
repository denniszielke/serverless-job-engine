param location string = resourceGroup().location
param logAnalyticsWorkspaceName string 
param appInsightsName string

resource appInsights 'Microsoft.Insights/components@2020-02-02-preview' = {
  name: appInsightsName
  location: location
  kind: 'web'
  properties: { 
    ApplicationId: appInsightsName
    Application_Type: 'web'
    Flow_Type: 'Redfield'
    Request_Source: 'CustomDeployment'
  }
}

resource logAnalyticsWorkspace 'Microsoft.OperationalInsights/workspaces@2020-08-01' = {
  name: logAnalyticsWorkspaceName
  location: location
  properties: any({
    retentionInDays: 30
    features: {
      searchVersion: 1
      legacy: 0
      enableLogAccessUsingOnlyResourcePermissions: true
    }
    sku: {
      name: 'PerGB2018'
    }
  })
}

resource azureMonitorWorkspace 'Microsoft.Monitor/accounts@2021-06-03-preview' = {
  name: logAnalyticsWorkspaceName
  location: location
  properties: { 
  }
}

resource grafana 'Microsoft.Dashboard/grafana@2022-08-01' = {
  name: logAnalyticsWorkspaceName
  location: location
  identity: {
    type: 'SystemAssigned'
  }
  sku: {
    name: 'Standard'
  }
  properties: {
    zoneRedundancy: 'Disabled'
    apiKey: 'Disabled'
    deterministicOutboundIP: 'Disabled'
    grafanaIntegrations: {
      azureMonitorWorkspaceIntegrations: [
        {
          azureMonitorWorkspaceResourceId: azureMonitorWorkspace.id
        }
      ]
    }
  }
}

output logAnalyticsCustomerId string = logAnalyticsWorkspace.properties.customerId
output logAnalyticsSharedKey string = logAnalyticsWorkspace.listKeys().primarySharedKey
output appInsightsInstrumentationKey string = appInsights.properties.InstrumentationKey
