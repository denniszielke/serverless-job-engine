# Create local telegraf test setup

```
DEPLOYMENT_NAME="dzaca67"

AZURE_TENANT_ID=$(az account show --query tenantId -o tsv)
SUBSCRIPTION_ID=$(az account show --query id -o tsv)

SERVICE_PRINCIPAL_ID=$(az ad sp create-for-rbac --role="Monitoring Metrics Publisher" --scopes="/subscriptions/${SUBSCRIPTION_ID}/resourcegroups/$DEPLOYMENT_NAME/providers/microsoft.monitor/accounts/logs-env-$DEPLOYMENT_NAME" --name $DEPLOYMENT_NAME-metrics  --sdk-auth -o json | jq -r '.clientId')

echo $SERVICE_PRINCIPAL_ID

SERVICE_PRINCIPAL_SECRET=$(az ad app credential reset --id $SERVICE_PRINCIPAL_ID -o json | jq '.password' -r)
echo $SERVICE_PRINCIPAL_SECRET

export LOCATION="westeurope"
export INSTANCE="engine"
export RESOURCE_ID="/subscriptions/$SUBSCRIPTION_ID/resourceGroups/$DEPLOYMENT_NAME/providers/Microsoft.App/containerapps/engine"
export PROMETHEUS_URL="http://localhost:8080/q/metrics"
export AZURE_CLIENT_ID="$SERVICE_PRINCIPAL_ID"
export AZURE_CLIENT_SECRET="$SERVICE_PRINCIPAL_SECRET"

~/lib/telegraf-1.25.1/usr/bin/telegraf --config ./telegraf/telegraf_monitor.conf
```

## Build telegraf container by using the existing docs:

https://github.com/influxdata/influxdata-docker/tree/master/telegraf

