param environmentName string
param location string = resourceGroup().location
param containerRegistryPath string
param storageAccountName string
param storageAccountKey string
param eventHubConnectionString string
param eventHubName string
param containerName string = 'output'
param queueName string = 'requests'
param stateName string = 'locks'

param clientId string
param clientSecret string
param tenantId string

resource uami 'Microsoft.ManagedIdentity/userAssignedIdentities@2018-11-30' = {
  name: 'engine-msi'
  location: location
}

resource state 'Microsoft.App/managedEnvironments/daprComponents@2022-01-01-preview' = {
  name: '${environmentName}/state'
  properties: {
    componentType : 'state.azure.tablestorage'
    version: 'v1'
    ignoreErrors: false
    initTimeout: '60s'
    secrets: [
      {
        name: 'storage-key'
        value: storageAccountKey
      }
    ]
    metadata : [
      {
        name: 'accountName'
        value: storageAccountName
      }
      {
        name: 'accountKey'
        secretRef: 'storage-key'
      }
      {
        name: 'tableName'
        value: stateName
      }
    ]
    scopes: [
      'engine'
    ]
  }
}

resource output 'Microsoft.App/managedEnvironments/daprComponents@2022-01-01-preview' = {
  name: '${environmentName}/output'
  properties: {
    componentType : 'bindings.azure.blobstorage'
    version: 'v1'
    ignoreErrors: false
    initTimeout: '60s'
    secrets: [
      {
        name: 'storage-key'
        value: storageAccountKey
      }
    ]
    metadata : [
      {
        name: 'storageAccount'
        value: storageAccountName
      }
      {
        name: 'storageAccessKey'
        secretRef: 'storage-key'
      }
      {
        name: 'container'
        value: containerName
      }            
      {
        name: 'decodeBase64'
        value: 'true'
      }
    ]
    scopes: [
      'engine'
    ]
  }
}

resource queueinput 'Microsoft.App/managedEnvironments/daprComponents@2022-01-01-preview' = {
  name: '${environmentName}/requests'
  properties: {
    componentType : 'pubsub.azure.eventhubs'
    version: 'v1'
    ignoreErrors: false
    initTimeout: '60s'
    secrets: [
      {
        name: 'eventhub-connectionstring'
        value: eventHubConnectionString
      }
      {
        name: 'storage-key'
        value: storageAccountKey
      }
    ]
    metadata : [
      {
        name: 'connectionString'
        secretRef: 'eventhub-connectionstring'
      }
      {
        name: 'storageAccountName'
        value: storageAccountName
      }  
      {
        name: 'storageAccountKey'
        secretRef: 'storage-key'
      }          
      {
        name: 'storageContainerName'
        value: 'checkpoints'
      }
    ]
    scopes: [
      'engine'
    ]
  }
}

resource containerApp 'Microsoft.App/containerapps@2022-01-01-preview' = {
  name: 'engine'
  kind: 'containerapp'
  location: location
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
        '${uami}': {}
    }
  }
  properties: {
    managedEnvironmentId: resourceId('Microsoft.App/managedEnvironments', environmentName)
    configuration: {
      activeRevisionsMode: 'single'
      ingress: {
        external: true
        targetPort: 8080
        allowInsecure: false    
        transport: 'Auto'
      }
      secrets: [
        {
          name: 'storage-connectionstring'
          value: 'DefaultEndpointsProtocol=https;AccountName=${storageAccountName};EndpointSuffix=core.windows.net;AccountKey=${storageAccountKey}'
        }
        {
          name: 'eventhub-connectionstring'
          value: eventHubConnectionString
        }
      ]
      dapr: {
        enabled: true
        appId: 'engine'
        appPort: 8080
        appProtocol: 'http'
      }
    }
    template: {
      containers: [
        {
          image: containerRegistryPath
          terminationGracePeriodSeconds: 5
          name: 'engine'
          resources: {
            cpu: '1.5'
            memory: '3Gi'
          }
          probes: [
            {
              type: 'liveness'
              httpGet: {
                path: '/ping'
                port: 8080
              }
              initialDelaySeconds: 5
              periodSeconds: 3
            }
            {
              type: 'readiness'
              httpGet: {
                path: '/ping'
                port: 8080
              }
              initialDelaySeconds: 5
              periodSeconds: 3
            }
          ]
          env:[
            {
              name: 'PORT'
              value: '8080'
            }
            {
              name: 'VALUE'
              value: '456'
            }
          ]
        }
        {
          image: 'denniszielke/telegraf'
          terminationGracePeriodSeconds: 5
          name: 'telegraf'
          resources: {
            cpu: '0.5'
            memory: '1Gi'
          }          
          env:[
            {
              name: 'AZURE_TENANT_ID'
              value: tenantId
            }
            {
              name: 'AZURE_CLIENT_ID'
              value: clientId
            }
            {
              name: 'AZURE_CLIENT_SECRET'
              value: clientSecret
            }
            {
              name: 'RESOURCE_ID'
              value: '${resourceGroup().id}/providers/Microsoft.App/containerapps/engine'
            }
            {
              name: 'LOCATION'
              value: location
            }
            {
              name: 'INSTANCE'
              value: 'engine'
            }
          ]
        }
      ]
      scale: {
        minReplicas: 1
        maxReplicas: 5
        rules: [
          {
            name: 'eventhub-based-autoscaling'
            custom: {
              type: 'azure-eventhub'
              metadata: {
                eventHubName: eventHubName
                consumerGroup: '$Default'
                blobContainer: 'checkpoints'
              }
              auth: [
                {
                  secretRef: 'eventhub-connectionstring'
                  triggerParameter: 'connection'
                }
                {
                  secretRef: 'storage-connectionstring'
                  triggerParameter: 'storageConnection'
                }
              ]
            }
          }
        ]
      }
    }
  }
}
