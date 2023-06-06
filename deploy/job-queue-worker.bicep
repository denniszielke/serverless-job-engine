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

resource uamijob 'Microsoft.ManagedIdentity/userAssignedIdentities@2018-11-30' = {
  name: 'job-engine-msi'
  location: location
}

resource containerJob2 'Microsoft.App/jobs@2022-11-01-preview' = {
  name: 'jobengine2'
  kind: 'containerapp'
  location: location
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${uamijob.id}': {}
    }
  }
  properties: {
    managedEnvironmentId: resourceId('Microsoft.App/managedEnvironments', environmentName)
    workloadProfileName: 'f4-compute'
    configuration: {
      replicaTimeout: 100
      replicaRetryLimit: 2
      manualTriggerConfig:  {
        replicaCompletionCount: 1
        parallelism: 4
      }
      triggerType: 'Manual' //'Schedule'
      // scheduleTriggerConfig: {
      //   replicaCompletionCount: 1
      //   cronExpression: '*/2 * * * *'
      //   parallelism: 1
      // }
      dapr: {
        enabled: true
        appId: 'jobengine'
        appPort: 8080
        appProtocol: 'http'
      }
    }
    template: {
      containers: [
        {
          image: containerRegistryPath
          // terminationGracePeriodSeconds: 5
          name: 'engine'
          resources: {
            cpu: '2'
            memory: '4Gi'
            ephemeralStorage: '2Gi'
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
          // env:[
          //   {
          //     name: 'PORT'
          //     value: '8080'
          //   }
          //   {
          //     name: 'VALUE'
          //     value: '4545'
          //   }
          // ]
        }
      ]
    }
  }
}
