param environmentName string
param location string = resourceGroup().location
param containerRegistryPath string
param storageAccountName string
param storageAccountKey string
param containerName string = 'output'
param queueName string = 'requests'
param stateName string = 'locks'

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

resource bloboutput 'Microsoft.App/managedEnvironments/daprComponents@2022-01-01-preview' = {
  name: '${environmentName}/bloboutput'
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
  name: '${environmentName}/queueinput'
  properties: {
    componentType : 'bindings.azure.storagequeues'
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
        name: 'queue'
        value: queueName
      }  
      {
        name: 'ttlInSeconds'
        value: '60'
      }          
      {
        name: 'decodeBase64'
        value: 'true'
      }
      {
        name: 'route'
        value: '/receive'
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
          name: 'engine'
          resources: {
            cpu: '2'
            memory: '4Gi'
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
          // volumeMounts: [
          //   {
          //     mountPath: '/mnt/files'
          //     volumeName: 'files'
          //   }
          // ]
          env:[
            {
              name: 'PORT'
              value: '8080'
            }
            {
              name: 'VALUE'
              value: '8080'
            }
          ]
        }
      ]
      scale: {
        minReplicas: 0
        maxReplicas: 5
        rules: [
          {
            name: 'queue-based-autoscaling'
            custom: {
              type: 'azure-queue'
              metadata: {
                queueName: queueName
                messageCount: '3'
              }
              auth: [
                {
                  secretRef: 'storage-connectionstring'
                  triggerParameter: 'connection'
                }
              ]
            }
          }
        ]
      }
    }
  }
}
