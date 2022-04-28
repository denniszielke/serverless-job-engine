!/bin/bash

set -e

# infrastructure deployment properties
DEPLOYMENT_NAME="$1" # here enter unique deployment name (ideally short and with letters for global uniqueness)
VERSION="$2" # version tag showing up in app
REGISTRY="$3"

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

AZURE_CORE_ONLY_SHOW_ERRORS="True"

az deployment group create -g $DEPLOYMENT_NAME -f deploy/apps.bicep \
          -p imageTag=$VERSION \
          -p containerRegistryOwner=$REGISTRY