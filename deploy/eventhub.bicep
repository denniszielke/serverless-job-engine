param location string = resourceGroup().location

param eventHubNamespaceName string
param eventHubName string

resource eventHubNamespace 'Microsoft.EventHub/namespaces@2021-11-01' = {
  name: eventHubNamespaceName
  location: location
  sku: {
    name: 'Standard'
    tier: 'Standard'
    capacity: 1
  }
  properties: {
    isAutoInflateEnabled: false
    maximumThroughputUnits: 0
  }
}

resource eventHub 'Microsoft.EventHub/namespaces/eventhubs@2021-11-01' = {
  parent: eventHubNamespace
  name: eventHubName
  properties: {
    messageRetentionInDays: 7
    partitionCount: 32
  }
}

resource eventHub_ListenSend 'Microsoft.EventHub/namespaces/eventhubs/authorizationRules@2021-01-01-preview' = {
  parent: eventHub
  name: 'ListenSend'
  properties: {
    rights: [
      'Listen'
      'Send'
    ]
  }
}

var eventHubNamespaceConnectionString = listKeys(eventHub_ListenSend.id, eventHub_ListenSend.apiVersion).primaryConnectionString
output eventHubNamespaceConnectionString string = '${eventHubNamespaceConnectionString};EntityPath=requests'
output eventHubName string = eventHubName
