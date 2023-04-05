# Serverless job processing on Azure Container Apps

This scenario project is about running a queue based job engine written in Java.
The objective is ensure the following requirements:
- Minimal amount of code beeing written by using Dapr
- Fast scale up time of container runtime by using Quarkus native images
- Automatic scale to 0 using Keda in Azure Container Apps
- Ensuring that each instance is only processing one job at a time
- Publishin application and runtime metrics via Micrometer
- Scrapping metrics via telegraf sidecar and publish them to Azure Monitor for Prometheus
- Using Azure Managed Grafana to build dashboards on jobengine execution times

![](/img/architecture.png)

## Local installation

### Install locally
- Install dapr using this guide https://docs.dapr.io/getting-started/install-dapr-cli/
- Init dapr using this guide https://docs.dapr.io/getting-started/install-dapr-selfhost/
- Install Quarkus using this guide https://quarkus.io/get-started/
- Add VSCode Extension for Quarkus https://marketplace.visualstudio.com/items?itemName=redhat.vscode-quarkus 

### Debug locally

- Start the "Debug Quarkus application (src/Engine) with Dapr Local Components" Debug mode in VSCode
- Trigger the Dapr input binding
```
curl -X POST -H 'Content-Type: application/json' http://localhost:8080/consume -d '{ message: "hello world" }'
```
- Create message in queue
```
curl -X POST -H 'Content-Type: application/json' http://localhost:8080/publish -d '{ message: "hello world" }'
```

## Deployment of the Azure resources

### Manual deployment of azure resources with azure cli

```
DEPLOYMENT_NAME="dzaca28" # here the deployment
LOCATION="northcentralus" # azure region 
bash ./deploy-aca-infra-bicep.sh $DEPLOYMENT_NAME $LOCATION
bash ./deploy-aks-infra-bicep.sh $DEPLOYMENT_NAME $LOCATION
```

### Manual deployment of app into existing Azure Container App Environment with azure cli

```
DEPLOYMENT_NAME="dzaca27" # here the deployment
LOCATION="westeurope" # azure region 
GHUSER="denniszielke" # replace with your user name
IMAGE_TAG="latest"
bash ./deploy-aca-apps-bicep.sh $DEPLOYMENT_NAME $GHUSER $IMAGE_TAG

```

### Debug locally with azure resource components

```
DEPLOYMENT_NAME="dzaca64" # here the deployment
bash ./create-config.sh $DEPLOYMENT_NAME

```

- Start the "Debug Quarkus application (src/Engine) with Dapr Azure Components" Debug mode in VSCode

### Publish messages locally

```
dapr publish --publish-app-id engine --topic requests --pubsub requests --data '{"guid":"balasbla", "message": "helloe"}'

```
## Set up workload Identity for your GitHub Actions to use federated trust

Official documentation:
https://docs.microsoft.com/en-us/azure/active-directory/develop/workload-identity-federation-create-trust-github?tabs=azure-portal

For this repository to your own account.

We will create a service principal and grant it permissions on a dedicated resource group

```
DEPLOYMENT_NAME="dzaca64" # here the deployment
RESOURCE_GROUP=$DEPLOYMENT_NAME # here enter the resources group
LOCATION="westeurope" # azure region 
AZURE_SUBSCRIPTION_ID=$(az account show --query id -o tsv) # here enter your subscription id
GHUSER="denniszielke" # replace with your user name
GHREPO="serverless-job-engine" # here the repo name
AZURE_TENANT_ID=$(az account show --query tenantId -o tsv)
GHREPO_BRANCH=":ref:refs/heads/main"
az group create -n $RESOURCE_GROUP -l $LOCATION -o none

AZURE_CLIENT_ID=$(az ad sp create-for-rbac --name "$DEPLOYMENT_NAME" --role contributor --scopes "/subscriptions/$AZURE_SUBSCRIPTION_ID/resourceGroups/$RESOURCE_GROUP" -o json | jq -r '.appId')

AZURE_CLIENT_OBJECT_ID="$(az ad app show --id ${AZURE_CLIENT_ID} --query objectId -o tsv)"

az rest --method POST --uri "https://graph.microsoft.com/beta/applications/$AZURE_CLIENT_OBJECT_ID/federatedIdentityCredentials" --body "{'name':'$DEPLOYMENT_NAME','issuer':'https://token.actions.githubusercontent.com','subject':'repo:$GHUSER/$GHREPO$GHREPO_BRANCH','description':'GitHub Actions for $DEPLOYMENT_NAME','audiences':['api://AzureADTokenExchange']}"
```

## Monitoring builtin and custom Java metrics using Micrometer
Using the telegraf sidecar we can public builtin and custom metrics via Micrometer, scrap the `/q/metrics` endpoint via a dedicated telegraf sidecar, send metrics to the Azure Monitor managed service for Prometheus (https://learn.microsoft.com/en-Us/azure/azure-monitor/essentials/prometheus-metrics-overview) and query the results via PromQL in Azure Managed Grafana:
![](/img/graph.png)