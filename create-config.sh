#!/bin/bash

set -e

# infrastructure deployment properties

DEPLOYMENT_NAME="$1" # here enter unique deployment name (ideally short and with letters for global uniqueness)

if [ "$DEPLOYMENT_NAME" == "" ]; then
echo "No project name provided - aborting"
exit 0;
fi

if [[ $DEPLOYMENT_NAME =~ ^[a-z0-9]{5,9}$ ]]; then
    echo "project name $DEPLOYMENT_NAME is valid"
else
    echo "project name $DEPLOYMENT_NAME is invalid - only numbers and lower case min 5 and max 8 characters allowed - aborting"
    exit 0;
fi

RESOURCE_GROUP="$DEPLOYMENT_NAME"

AZURE_CORE_ONLY_SHOW_ERRORS="True"

if [ $(az group exists --name $RESOURCE_GROUP) = false ]; then
    echo "resource group $RESOURCE_GROUP does not exist"
    error=1
else   
    echo "resource group $RESOURCE_GROUP already exists"
    LOCATION=$(az group show -n $RESOURCE_GROUP --query location -o tsv)
fi

STORAGE_KEY=$(az storage account keys list --account-name strg$DEPLOYMENT_NAME --resource-group $RESOURCE_GROUP --query "[0].value" -o tsv)
STORAGE_NAME="strg$DEPLOYMENT_NAME"

STORAGE_KEY_ESC=$(echo $STORAGE_KEY | sed 's/\//\\\//g')

replaces="s/{.storageAccount}/$STORAGE_NAME/;";
replaces="$replaces s/{.storageAccessKey}/$STORAGE_KEY_ESC/; ";

mkdir -p components

cat ./component-templates/output-template.yaml | sed -e "$replaces" > ./components/output.yaml
cat ./component-templates/queue-template.yaml | sed -e "$replaces" > ./components/queue.yaml
cat ./component-templates/state-template.yaml | sed -e "$replaces" > ./components/state.yaml

echo "create component files in /components directory"