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
  ./check-dot-env.sh
  . ../setup/get-config.sh

  ../docker/build-all.sh
  if [ "$IS_REMOTE" = true ]; then
    . ../aws/env.sh
    ../aws/push-images.sh
    export BASIC_AUTH=true
    export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
    export IMAGE_REPO_PREFIX="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/"
  else
    export BASIC_AUTH=false
  fi
}

setup_k8s_secrets() {
  echo "Setting up Kubernetes secrets..."
  kubectl delete secret $APP_NAME-secrets --namespace $NAMESPACE --ignore-not-found
  kubectl create secret generic $APP_NAME-secrets --namespace $NAMESPACE --from-env-file="$ENV_FILE"

  kubectl delete secret basic-auth --namespace $NAMESPACE --ignore-not-found
  printf '%s:%s\n' "$USERNAME_TOOLS" "$(openssl passwd -apr1 "$PASSWORD_TOOLS")" >"$AUTH_FILE"
  kubectl create secret generic basic-auth --namespace $NAMESPACE --from-file=auth="$AUTH_FILE"
  rm -f "$AUTH_FILE"
}

setup_tls_secret() {
  # No real domain is available (only a bare/nip.io public IP), so a CA-trusted
  # cert (Let's Encrypt/ACM) isn't an option here - a self-signed cert at least
  # encrypts the wire (Basic Auth credentials, API traffic) instead of sending
  # everything in cleartext.
  echo "Setting up self-signed TLS certificate..."
  local cert_file="$HELM_SCRIPT_DIR/../../configs/.tls.crt"
  local key_file="$HELM_SCRIPT_DIR/../../configs/.tls.key"

  openssl req -x509 -newkey rsa:2048 -nodes \
    -keyout "$key_file" -out "$cert_file" \
    -days 825 \
    -subj "/CN=${PUBLIC_IP}" \
    -addext "subjectAltName=DNS:${PUBLIC_IP},DNS:*.${PUBLIC_IP}"

  kubectl delete secret $APP_NAME-tls --namespace $NAMESPACE --ignore-not-found
  kubectl create secret tls $APP_NAME-tls --namespace $NAMESPACE --cert="$cert_file" --key="$key_file"
  rm -f "$cert_file" "$key_file"
}

clean_previous_installation() {
  echo "Uninstalling previous installation..."
  "$HELM_SCRIPT_DIR/uninstall.sh" "$@"
  find "$CONFIG_DIR/charts" -mindepth 1 -maxdepth 1 ! -name '.gitignore' -exec rm -rf {} +
  echo "Creating namespace..."
  kubectl get namespace $NAMESPACE || kubectl create namespace $NAMESPACE
}

install_dependencies() {
  echo "Installing dependencies..."
  . "$HELM_SCRIPT_DIR/install-nginx.sh"
  envsubst '${APP_NAME} ${NAMESPACE} ${IMAGE_REPO_PREFIX} ${IMAGE_TAG} ${MINIO_ADDR} ${USERNAME_TOOLS} ${PASSWORD_TOOLS} ${PUBLIC_IP} ${BASIC_AUTH}' <"$CONFIG_DIR/template.values.yaml" >"$CONFIG_DIR/values.yaml"
  helm dependency update --namespace $NAMESPACE
}

install_and_run_tests() {
  echo "Running test suite..."
  "$HELM_SCRIPT_DIR/test.sh"

  tls_args=()
  if [ "$IS_REMOTE" = true ]; then
    tls_args=(
      --set-json "ingress.tls=[{\"secretName\":\"$APP_NAME-tls\",\"hosts\":[\"$PUBLIC_IP\"]}]"
      --set-json "minio.consoleIngress.tls=[{\"secretName\":\"$APP_NAME-tls\",\"hosts\":[\"minio.$PUBLIC_IP\"]}]"
      --set-json "grafana.ingress.tls=[{\"secretName\":\"$APP_NAME-tls\",\"hosts\":[\"grafana.$PUBLIC_IP\"]}]"
    )
  fi

  echo "Installing main app..."
  helm upgrade --install $APP_NAME "$CONFIG_DIR" \
    -f "$CONFIG_DIR/values.yaml" \
    --namespace $NAMESPACE \
    --set global.imageRepoPrefix="$IMAGE_REPO_PREFIX" \
    "${tls_args[@]}"

  echo "Waiting for pods to become ready..."
  kubectl get pods --namespace $NAMESPACE --no-headers \
  -l 'helm.sh/hook!=test' | awk '{print $1}' |
  xargs -I {} kubectl wait --for=condition=ready pod {} --namespace $NAMESPACE --timeout=300s


  test_passed=false
  for i in {1..10}; do
    echo "Attempt $i/10 ..."
    sleep 10
    if helm test "$APP_NAME" --namespace "$NAMESPACE"; then
      test_passed=true
      break
    fi
  done

  kubectl delete pod -l helm.sh/hook=test --namespace "$NAMESPACE"

  if [ "$test_passed" != true ]; then
    echo "Integration test failed after 10 attempts." >&2
    exit 1
  fi

  echo "Integration test passed."
}

print_access_urls() {
  local scheme="http"
  local suffix="${SUFFIX}"
  if [ "$IS_REMOTE" = true ]; then
    scheme="https"
    suffix=":$EC2_PORT_SSL"
  fi

  echo -e "\e[1m✅ All components installed. Access URLs:\e[0m"
  echo -e "\e[36m🔹 App:      ${scheme}://${PUBLIC_IP}${suffix}\e[0m"
  echo -e "\e[33m🔹 Grafana:  ${scheme}://grafana.${PUBLIC_IP}${suffix}\e[0m"
  echo -e "\e[35m🔹 MinIO:    ${scheme}://minio.${PUBLIC_IP}${suffix}\e[0m"
  echo ""
  echo "In a minikube environment a 'kubectl port-forward svc/ingress-nginx-controller 8080:80' might be required."
}

source_env_and_build
pushd "$CONFIG_DIR" >/dev/null
clean_previous_installation "$@"
setup_k8s_secrets
install_dependencies
if [ "$IS_REMOTE" = true ]; then
  setup_tls_secret
fi
install_and_run_tests
print_access_urls
popd >/dev/null

popd >/dev/null
