param environmentName string
param location string = resourceGroup().location
param containerRegistryPath string
param storageAccountName string
param storageAccountKey string
param containerName string = 'output'
param queueName string = 'requests'

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
      'optimizer'
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
    ]
    scopes: [
      'optimizer'
    ]
  }
}

resource containerApp 'Microsoft.App/containerapps@2022-01-01-preview' = {
  name: 'optimizer'
  kind: 'containerapp'
  location: location
  properties: {
    managedEnvironmentId: resourceId('Microsoft.App/managedEnvironments', environmentName)
    configuration: {
      activeRevisionsMode: 'single'
      ingress: {
        external: true
        targetPort: 6000
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
        appId: 'optimizer'
        appPort: 6000
        appProtocol: 'http'
      }
    }
    template: {
      containers: [
        {
          image: containerRegistryPath
          name: 'optimizer'
          resources: {
            cpu: '2'
            memory: '4Gi'
          }
          probes: [
            {
              type: 'liveness'
              httpGet: {
                path: '/ping'
                port: 6000
              }
              initialDelaySeconds: 5
              periodSeconds: 3
            }
            {
              type: 'readiness'
              httpGet: {
                path: '/ping'
                port: 6000
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
