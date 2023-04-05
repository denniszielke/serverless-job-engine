@description('Specifies the Azure location for all resources.')
param location string = resourceGroup().location

param serviceBusName string 

resource serviceBus 'Microsoft.ServiceBus/namespaces@2021-06-01-preview' = {
  name: serviceBusName
  location: location
  sku: {
    name: 'Standard'
    tier: 'Standard'
    capacity: 1
  }
}

resource topicmessages 'Microsoft.ServiceBus/namespaces/topics@2022-10-01-preview' = {
  name: 'messages'
  parent: serviceBus
  properties: {
  }
}

resource subscriptionb 'Microsoft.ServiceBus/namespaces/topics/subscriptions@2022-10-01-preview' = {
  name: 'message-receiver'
  parent: topicmessages
  properties: {
  }
}

