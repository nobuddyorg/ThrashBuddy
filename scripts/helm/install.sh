#!/bin/bash
# Description: [-remote] Install or Update all Helm charts, including thrash-buddy itself (local or remote cluster).

set -eu

pushd "$(dirname "$0")" >/dev/null

IS_REMOTE=false
for arg in "$@"; do
  case "$arg" in
  -remote) IS_REMOTE=true ;;
  esac
done

./uninstall.sh "$@"
../docker/build-all.sh

if [ "$IS_REMOTE" = true ]; then
  ../aws/push-images.sh
fi

HELM_DIR="$(pwd)"
ENV_FILE="$HELM_DIR/../../configs/.env"
AUTH_FILE="$HELM_DIR/../../configs/.auth"

./check-env.sh
. "$ENV_FILE"
./test.sh

pushd ../../charts >/dev/null

echo "Setting up AWS credentials..."
kubectl delete secret thrashbuddy-secrets || true
kubectl create secret generic thrashbuddy-secrets --from-env-file="$ENV_FILE"

kubectl delete secret basic-auth || true
printf "$USERNAME_TOOLS:$(openssl passwd -apr1 $PASSWORD_TOOLS)\n" >$AUTH_FILE
kubectl create secret generic basic-auth --from-file=auth=$AUTH_FILE
if [ "$IS_REMOTE" = true ]; then
  . $HELM_DIR/../aws/env.sh
  AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
  IMAGE_REPO_PREFIX="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/"
  $HELM_DIR/install-external-charts.sh -remote
  . $HELM_DIR/ip.sh
  PUBLIC_IP=${PUBLIC_IP}.nip.io
else
  IMAGE_REPO_PREFIX=""
  $HELM_DIR/install-external-charts.sh
  PUBLIC_IP=localhost
fi

helm upgrade --install thrash-buddy . \
  -f values.yaml \
  --set deployments.api.env.MINIO_ACCESS_KEY="$USERNAME_TOOLS" \
  --set deployments.api.env.MINIO_SECRET_KEY="$PASSWORD_TOOLS" \
  --set global.imageRepoPrefix="$IMAGE_REPO_PREFIX" \
  --set ingress.host="${PUBLIC_IP}" \
  --set ingress.basicauth=$([ "$IS_REMOTE" = "true" ] && echo true || echo false)

echo "integration test running..."
kubectl get pods -n default --no-headers | grep -vE 'test-' | awk '{print $1}' |
  xargs -I {} kubectl wait --for=condition=ready pod {} -n default --timeout=300s

for i in {1..10}; do
  echo "Attempt $i/10 ..."
  sleep 10
  if helm test thrash-buddy; then
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
echo ""
echo "In a minikube environment a 'kubectl port-forward svc/ingress-nginx-controller 8080:80' might be required."

popd >/dev/null
popd >/dev/null
