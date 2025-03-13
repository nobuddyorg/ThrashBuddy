#!/bin/bash

set -e

# navigate to the correct path for script execution and execute the scripts
pushd $(dirname $0)/../../ > /dev/null
pushd services/frontend/

echo "==========================================="
echo "Start building image for frontend:"
echo "==========================================="
docker build -t cloud-thrash/frontend .


# navigate back to start dir to ensure the working dir stays the same after exectuing the script
popd
popd > /dev/null
