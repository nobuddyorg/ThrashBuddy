#!/bin/bash

set -e
pushd $(dirname $0) > /dev/null

. ./.env

echo "(Re-)Installing ingress-nginx..."
helm repo add ingress-nginx-repo https://kubernetes.github.io/ingress-nginx
helm repo update
helm upgrade --install ingress-nginx ingress-nginx-repo/ingress-nginx \
  --set controller.resources.requests.cpu=200m \
  --set controller.resources.requests.memory=192Mi \
  --set controller.resources.limits.cpu=200m \
  --set controller.resources.limits.memory=192Mi \
  --set controller.replicaCount=1 \
  --set controller.podDisruptionBudget.enabled=true \
  --set controller.podDisruptionBudget.minAvailable=1 \
  --set controller.service.type=LoadBalancer \
  --set controller.service.annotations."service\.beta\.kubernetes\.io/aws-load-balancer-type"="external"

echo "Waiting for ingress-nginx controller pod..."
kubectl wait --namespace default \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=90s

echo "(Re-)Installing minio..."
helm repo add minio https://charts.min.io/
helm repo update
helm upgrade --install minio minio/minio \
  --set resources.requests.memory=256Mi \
  --set resources.requests.cpu=250m \
  --set resources.limits.memory=256Mi \
  --set resources.limits.cpu=250m \
  --set replicas=1 \
  --set persistence.enabled=false \
  --set mode=standalone \
  --set rootUser=$USERNAME_TOOLS,rootPassword=$PASSWORD_TOOLS \
  --set service.type=ClusterIP \
  --set buckets[0].name=cloud-thrash,buckets[0].policy=none,buckets[0].purge=false \
  --set podDisruptionBudget.enabled=true \
  --set podDisruptionBudget.minAvailable=1 \
  --set consoleIngress.enabled=true \
  --set consoleIngress.hosts[0]=minio.localhost \
  --set consoleIngress.ingressClassName=nginx \
  --set "consoleIngress.path='/'" \
  --set consoleIngress.pathType=Prefix
kubectl patch ingress minio-console -p '{"spec":{"ingressClassName":"nginx"}}'


echo "(Re-)Installing InfluxDB..."
helm repo add influxdata https://helm.influxdata.com/
helm repo update
helm upgrade --install influxdb influxdata/influxdb2 \
  --set resources.requests.memory=1Gi \
  --set resources.requests.cpu=500m \
  --set resources.limits.memory=1Gi \
  --set resources.limits.cpu=500m \
  --set persistence.enabled=false \
  --set adminUser.organization=cloud-thrash \
  --set adminUser.bucket=metrics \
  --set adminUser.user=$USERNAME_TOOLS \
  --set adminUser.password=$PASSWORD_TOOLS \
  --set adminUser.token=$INFLUXDB_API_TOKEN \
  --set ingress.enabled=true \
  --set ingress.hostname=influx.localhost \
  --set "ingress.path='/'" \
  --set ingress.pathType=Prefix \
  --set ingress.ingressClassName=nginx
kubectl patch ingress influxdb-influxdb2 -p '{"spec":{"ingressClassName":"nginx"}}'

echo "(Re-)Installing Grafana..."
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update
helm upgrade --install grafana grafana/grafana \
  --set adminUser=$USERNAME_TOOLS \
  --set adminPassword=$PASSWORD_TOOLS \
  --set datasources.datasources\\.yaml.datasources[0].secureJsonData.token=$INFLUXDB_API_TOKEN \
  --set replicaCount=1 \
  --set resources.requests.cpu=200m \
  --set resources.requests.memory=256Mi \
  --set resources.limits.cpu=200m \
  --set resources.limits.memory=256Mi \
  --set ingress.enabled=true \
  --set ingress.ingressClassName=nginx \
  --set ingress.hosts[0]=grafana.localhost \
  --set "ingress.path='/'" \
  --set ingress.pathType=Prefix \
  --values grafana-values.yaml
kubectl patch ingress grafana -p '{"spec":{"ingressClassName":"nginx"}}'

popd > /dev/null
