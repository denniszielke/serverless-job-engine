param location string = resourceGroup().location
param environmentName string
param workspaceResourceId string
param vmSize string = 'standard_d2s_v3'

resource aks 'Microsoft.ContainerService/managedClusters@2023-01-02-preview' = {
  name: environmentName
  location: location
  identity: {
    type: 'SystemAssigned'
  }
  sku: {
    name: 'Basic'
    tier: 'Paid'
  }
  properties: {
    dnsPrefix: environmentName
    enableRBAC: true
    ingressProfile: {
      webAppRouting: {
        enabled: true
      }
    }
    workloadAutoScalerProfile: {
      keda: {
        enabled: true
      }
    }
    agentPoolProfiles: [
      {
        availabilityZones: [
          '1'
          '2'
          '3'
        ]
        name: 'default'
        enableAutoScaling: true
        scaleDownMode: 'Deallocate'
        scaleSetEvictionPolicy: 'Deallocate'
        count: 4
        minCount: 1
        maxCount: 3
        vmSize: vmSize
        mode: 'System'
        nodeLabels: {
          workload: 'system'
        }
        osType: 'Linux'
        osSKU: 'Mariner'
      }
      {
        availabilityZones: [
          '1'
          '2'
          '3'
        ]
        name: 'spot'
        enableAutoScaling: true
        scaleDownMode: 'Deallocate'
        scaleSetEvictionPolicy: 'Deallocate'
        scaleSetPriority: 'Spot'
        spotMaxPrice: -1
        count: 1
        minCount: 0
        maxCount: 3
        vmSize: vmSize
        mode: 'User'
        nodeLabels: {
          workload: 'scale'
        }
      }
    ]
    networkProfile: {
      networkPlugin: 'azure'

    }
    autoScalerProfile: {
      expander: 'least-waste'
      'max-graceful-termination-sec': '100'
      'max-node-provision-time': '5m'
      'ok-total-unready-count': '1'
      'scale-down-delay-after-add': '3m'
      'scale-down-unneeded-time': '5m'
      'scale-down-utilization-threshold': '0.5'
      'scan-interval': '10s'
    }
    addonProfiles: {
      omsagent: {
        enabled: true
        config: {
          logAnalyticsWorkspaceResourceID: workspaceResourceId
        }
      }
    }
  }
}
