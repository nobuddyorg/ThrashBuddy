#!/bin/bash
# Description: [-remote] Install or Update all Helm charts, including thrash-buddy itself (local or remote cluster).

set -e

pushd "$(dirname "$0")" >/dev/null

. ./parse-args.sh

echo "Build images..."
../docker/build-all.sh

if [ "$IS_REMOTE" = true ]; then
  echo "Remote cluster detected. Setting up remote environment..."
  ../aws/push-images.sh
  export BASIC_AUTH=true
else
  echo "Local cluster detected. Setting up local environment..."
  export BASIC_AUTH=false
fi

HELM_SCRIPT_DIR="$(pwd)"
ENV_FILE="$HELM_SCRIPT_DIR/../../configs/.env"
AUTH_FILE="$HELM_SCRIPT_DIR/../../configs/.auth"

./check-env.sh
. "$ENV_FILE"
. ../setup/get-config.sh

pushd ../../charts >/dev/null

echo "Setting up secrets..."
kubectl delete secret thrashbuddy-secrets || true
kubectl create secret generic thrashbuddy-secrets --from-env-file="$ENV_FILE"
kubectl delete secret basic-auth || true
printf "$USERNAME_TOOLS:$(openssl passwd -apr1 $PASSWORD_TOOLS)\n" >$AUTH_FILE
kubectl create secret generic basic-auth --from-file=auth=$AUTH_FILE

echo "Uninstalling previous installation..."
$HELM_SCRIPT_DIR/uninstall.sh "$@"

echo "Installing dependencies..."
. $HELM_SCRIPT_DIR/install-nginx.sh
envsubst <template.values.yaml >values.yaml
helm dependency update

echo "Installing thrash-buddy..."
#$HELM_SCRIPT_DIR/test.sh
helm upgrade --install thrash-buddy . -f values.yaml

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
