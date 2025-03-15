#!/bin/bash

pushd $(dirname $0) > /dev/null

. ./.env
./test.sh

echo "(Re-)Installing ingress-nginx..."
helm repo add ingress-nginx-repo https://kubernetes.github.io/ingress-nginx
helm repo update
helm upgrade --install ingress-nginx ingress-nginx-repo/ingress-nginx \
  --set controller.podDisruptionBudget.enabled=true \
  --set controller.podDisruptionBudget.minAvailable=1

sleep 5

echo "(Re-)Installing minio..."
helm repo add minio https://charts.min.io/
helm repo update
helm upgrade --install minio minio/minio \
  --set resources.requests.memory=512Mi \
  --set replicas=1 \
  --set persistence.enabled=false \
  --set mode=standalone \
  --set rootUser=$AWS_ACCESS_KEY_ID,rootPassword=$AWS_SECRET_ACCESS_KEY \
  --set service.type=ClusterIP \
  --set consoleService.type=NodePort \
  --set buckets[0].name=cloud-thrash,buckets[0].policy=none,buckets[0].purge=false \
  --set podDisruptionBudget.enabled=true \
  --set podDisruptionBudget.minAvailable=1

helm upgrade --install cloud-thrash .

popd > /dev/null
