#!/bin/bash

set -e
pushd $(dirname $0) > /dev/null

# Parse arguments
IS_REMOTE=false
for arg in "$@"; do
  if [ "$arg" = "-remote" ]; then
    IS_REMOTE=true
  fi
done

. ./.env

echo "(Re-)Installing ingress-nginx..."
helm repo add ingress-nginx-repo https://kubernetes.github.io/ingress-nginx
helm repo update

if [ "$IS_REMOTE" = true ]; then
  helm upgrade --install ingress-nginx ingress-nginx-repo/ingress-nginx \
    --namespace ingress-ec2 \
    --create-namespace \
    --set controller.resources.requests.cpu=200m \
    --set controller.resources.requests.memory=192Mi \
    --set controller.resources.limits.cpu=200m \
    --set controller.resources.limits.memory=192Mi \
    --set controller.replicaCount=1 \
    --set controller.podDisruptionBudget.enabled=true \
    --set controller.podDisruptionBudget.minAvailable=1 \
    --set controller.nodeSelector.dedicated=ingress \
    --set controller.ingressClass=nginx \
    --set controller.service.type=NodePort \
    --set controller.service.nodePorts.http=30080 \
    --set controller.service.nodePorts.https=30443

  echo "⏳ Waiting for ingress-nginx controller pod..."
  kubectl wait --namespace ingress-ec2 \
    --for=condition=ready pod \
    --selector=app.kubernetes.io/component=controller \
    --timeout=90s

  . ./ip.sh
  PUBLIC_IP=${PUBLIC_IP}.nip.io
else
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

  PUBLIC_IP=localhost
fi

# ===============================
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
  --set consoleIngress.hosts[0]=minio.${PUBLIC_IP} \
  --set consoleIngress.ingressClassName=nginx \
  --set "consoleIngress.path='/'" \
  --set consoleIngress.pathType=Prefix
kubectl patch ingress minio-console -p '{"spec":{"ingressClassName":"nginx"}}'

# ===============================
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
  --set ingress.hostname=influx.${PUBLIC_IP} \
  --set "ingress.path='/'" \
  --set ingress.pathType=Prefix \
  --set ingress.ingressClassName=nginx
kubectl patch ingress influxdb-influxdb2 -p '{"spec":{"ingressClassName":"nginx"}}'

# ===============================
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
  --set ingress.hosts[0]=grafana.${PUBLIC_IP} \
  --set "ingress.path='/'" \
  --set ingress.pathType=Prefix \
  --values grafana-values.yaml
kubectl patch ingress grafana -p '{"spec":{"ingressClassName":"nginx"}}'

popd > /dev/null
