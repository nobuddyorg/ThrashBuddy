#!/bin/bash

set -e
pushd $(dirname "$0") > /dev/null

# Parse arguments
IS_REMOTE=false
for arg in "$@"; do
  if [ "$arg" = "-remote" ]; then
    IS_REMOTE=true
  fi
done

./check-env.sh
. ./.env
./test.sh

echo "Setting up AWS credentials..."
kubectl delete secret cloudthrash-secrets || true
kubectl create secret generic cloudthrash-secrets --from-env-file=.env

kubectl delete secret basic-auth || true
printf "$USERNAME_TOOLS:$(openssl passwd -apr1 $PASSWORD_TOOLS)\n" > .auth
kubectl create secret generic basic-auth --from-file=auth=.auth

if [ "$IS_REMOTE" = true ]; then
  . ../aws/env.sh
  AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
  IMAGE_REPO_PREFIX="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/"
  ./install-external-charts.sh -remote
  . ./ip.sh
  PUBLIC_IP=${PUBLIC_IP}.nip.io
else
  IMAGE_REPO_PREFIX=""
  ./install-external-charts.sh
  PUBLIC_IP=localhost
fi

helm upgrade --install cloud-thrash . \
  -f values.yaml \
  --set deployments.backend.env.MINIO_ACCESS_KEY="$USERNAME_TOOLS" \
  --set deployments.backend.env.MINIO_SECRET_KEY="$PASSWORD_TOOLS" \
  --set global.imageRepoPrefix="$IMAGE_REPO_PREFIX" \
  --set ingress.host="${PUBLIC_IP}" \
  --set ingress.basicauth=${IS_REMOTE:+"false"}

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

SUFFIX="${SUFFIX}"
if [ "$IS_REMOTE" = true ]; then
  SUFFIX=":30080"
fi

echo -e "\e[1m✅ All components installed. Access URLs:\e[0m"
echo -e "\e[36m🔹 App:      http://${PUBLIC_IP}${SUFFIX}\e[0m"
echo -e "\e[33m🔹 Grafana:  http://grafana.${PUBLIC_IP}${SUFFIX}\e[0m"
echo -e "\e[35m🔹 MinIO:    http://minio.${PUBLIC_IP}${SUFFIX}\e[0m"
echo -e "\e[34m🔹 InfluxDB: http://influx.${PUBLIC_IP}${SUFFIX}\e[0m"

popd > /dev/null
