#!/bin/bash

set -e
pushd $(dirname $0) > /dev/null

. ./.env
./test.sh

echo "(Re-)Installing ingress-nginx..."
helm repo add ingress-nginx-repo https://kubernetes.github.io/ingress-nginx
helm repo update
helm upgrade --install ingress-nginx ingress-nginx-repo/ingress-nginx \
  --set controller.podDisruptionBudget.enabled=true \
  --set controller.podDisruptionBudget.minAvailable=1

sleep 10

echo "(Re-)Installing minio..."
helm repo add minio https://charts.min.io/
helm repo update
helm upgrade --install minio minio/minio \
  --set resources.requests.memory=512Mi \
  --set replicas=1 \
  --set persistence.enabled=false \
  --set mode=standalone \
  --set rootUser=$USERNAME,rootPassword=$PASSWORD \
  --set service.type=ClusterIP \
  --set consoleService.type=NodePort \
  --set consoleService.nodePort=32001 \
  --set buckets[0].name=cloud-thrash,buckets[0].policy=none,buckets[0].purge=false \
  --set podDisruptionBudget.enabled=true \
  --set podDisruptionBudget.minAvailable=1

echo "(Re-)Installing InfluxDB..."
helm repo add influxdata https://helm.influxdata.com/
helm repo update
helm upgrade --install influxdb influxdata/influxdb2 \
  --set persistence.enabled=false \
  --set adminUser.organization=cloud-thrash \
  --set adminUser.bucket=metrics \
  --set adminUser.user=$USERNAME \
  --set adminUser.password=$PASSWORD \
  --set adminUser.token=$INFLUXDB_API_TOKEN \
  --set service.type=NodePort \
  --set service.nodePort=32002

helm upgrade --install cloud-thrash .

echo "integration test running..."
kubectl get pods -n default --no-headers | grep -vE 'test-' | awk '{print $1}' | \
    xargs -I {} kubectl wait --for=condition=ready pod {} -n default --timeout=300s

for i in {1..10}; do
    echo "Attempt $i/10 ..."
    sleep 10
    if helm test cloud-thrash; then
        break
    fi
done

kubectl delete pod -l helm.sh/hook=test
echo "integration test passed."

popd > /dev/null
