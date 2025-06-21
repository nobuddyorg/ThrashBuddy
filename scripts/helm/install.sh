#!/bin/bash
# Description: [-remote] Install or Update all Helm charts, including main app itself (local or remote cluster).

set -e

IS_REMOTE=false
for arg in "$@"; do
  case "$arg" in
  -remote) IS_REMOTE=true ;;
  esac
done

pushd "$(dirname "$0")" >/dev/null

HELM_SCRIPT_DIR="$(pwd)"
ENV_FILE="$HELM_SCRIPT_DIR/../../configs/.env"
AUTH_FILE="$HELM_SCRIPT_DIR/../../configs/.auth"
CONFIG_DIR="$HELM_SCRIPT_DIR/../../configs/helm"

source_env_and_build() {
  ../docker/build-all.sh
  if [ "$IS_REMOTE" = true ]; then
    ../aws/push-images.sh
    export BASIC_AUTH=true
  else
    export BASIC_AUTH=false
  fi

  ./check-env.sh
  . ../setup/get-config.sh
}

setup_k8s_secrets() {
  echo "Setting up Kubernetes secrets..."
  kubectl delete secret $APP_NAME-secrets --namespace $NAMESPACE --ignore-not-found
  kubectl create secret generic $APP_NAME-secrets --namespace $NAMESPACE --from-env-file="$ENV_FILE"

  kubectl delete secret basic-auth --namespace $NAMESPACE --ignore-not-found
  printf "$USERNAME_TOOLS:$(openssl passwd -apr1 "$PASSWORD_TOOLS")\n" >"$AUTH_FILE"
  kubectl create secret generic basic-auth --namespace $NAMESPACE --from-file=auth="$AUTH_FILE"
}

clean_previous_installation() {
  echo "Uninstalling previous installation..."
  "$HELM_SCRIPT_DIR/uninstall.sh" "$@"
  echo "Creating namespace..."
  kubectl create namespace $NAMESPACE
}

install_dependencies() {
  echo "Installing dependencies..."
  find "$CONFIG_DIR/charts" -mindepth 1 -maxdepth 1 ! -name '.gitignore' -exec rm -rf {} +
  . "$HELM_SCRIPT_DIR/install-nginx.sh"
  envsubst <"$CONFIG_DIR/template.values.yaml" >"$CONFIG_DIR/values.yaml"
  helm dependency update --namespace $NAMESPACE
}

install_and_run_tests() {
  echo "Running test suite..."
  "$HELM_SCRIPT_DIR/test.sh"

  echo "Installing main app..."
  helm upgrade --install $APP_NAME "$CONFIG_DIR" -f "$CONFIG_DIR/values.yaml" --namespace $NAMESPACE

  echo "Waiting for pods to become ready..."
  kubectl get pods --namespace $NAMESPACE --no-headers | grep -vE 'test-' | awk '{print $1}' |
    xargs -I {} kubectl wait --for=condition=ready pod {} --namespace $NAMESPACE --timeout=300s

  for i in {1..10}; do
    echo "Attempt $i/10 ..."
    sleep 10
    if helm test $APP_NAME --namespace $NAMESPACE; then
      break
    fi
  done

  kubectl delete pod -l helm.sh/hook=test --namespace $NAMESPACE
  echo "Integration test passed."
}

print_access_urls() {
  local suffix="${SUFFIX}"
  if [ "$IS_REMOTE" = true ]; then
    suffix=":30080"
  fi

  echo -e "\e[1m✅ All components installed. Access URLs:\e[0m"
  echo -e "\e[36m🔹 App:      http://${PUBLIC_IP}${suffix}\e[0m"
  echo -e "\e[33m🔹 Grafana:  http://grafana.${PUBLIC_IP}${suffix}\e[0m"
  echo -e "\e[35m🔹 MinIO:    http://minio.${PUBLIC_IP}${suffix}\e[0m"
  echo -e "\e[34m🔹 InfluxDB: http://influx.${PUBLIC_IP}${suffix}\e[0m"
  echo ""
  echo "In a minikube environment a 'kubectl port-forward svc/ingress-nginx-controller 8080:80' might be required."
}

source_env_and_build
pushd "$CONFIG_DIR" >/dev/null
clean_previous_installation "$@"
setup_k8s_secrets
install_dependencies
install_and_run_tests
print_access_urls
popd >/dev/null

popd >/dev/null
