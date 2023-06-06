param environmentName string
param location string = resourceGroup().location

//az containerapp job create echo -g $DEPLOYMENT_NAME --trigger-type Manual --replica-timeout 100 --replica-retry-limit 2 --replica-count 1 --parallelism 1 --image busybox --environment $DEPLOYMENT_NAME

resource uamiecho 'Microsoft.ManagedIdentity/userAssignedIdentities@2018-11-30' = {
  name: 'job-echo-engine-msi'
  location: location
}

resource echojob2 'Microsoft.App/jobs@2022-11-01-preview' = {
  name: 'echojob2'
  kind: 'containerapp'
  location: location
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${uamiecho.id}': {}
    }
  }
  properties: {
    managedEnvironmentId: resourceId('Microsoft.App/managedEnvironments', environmentName)
    workloadProfileName: 'consumption'
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
    }
    template: {
      initContainers: [
        {
          image: 'busybox'
          name: 'busybox'
          resources: {
            cpu: '1'
            memory: '1Gi'
          }
          command: [
            '/bin/sh'
          ]
          args: [
            '-c'
            'echo init; sleep 10; echo initended'
          ]
        }
      ]
      containers: [
        {
          image: 'busybox'
          name: 'busybox'
          resources: {
            cpu: '1'
            memory: '3Gi'
            ephemeralStorage: '2Gi'
          }
          command: [
            '/bin/sh'
          ]
          args: [
            '-c'
            'echo hello; sleep 100; echo helloended'
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
