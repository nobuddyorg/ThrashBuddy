#!/bin/bash

set -e
pushd $(dirname $0) > /dev/null

kubectl get nodes --no-headers -o custom-columns=":status.conditions[-1].type"
IMAGES=$(docker images --format "{{.Repository}}:{{.Tag}}" | grep -v "<none>" | grep "^cloud-thrash/")
for IMAGE in $IMAGES; do
    echo "Loading image: $IMAGE"
    minikube image load "$IMAGE"
done

popd > /dev/null
