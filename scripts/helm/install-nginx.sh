#!/bin/bash

set -e

pushd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null

function wait_for_ingress_controller() {
  echo "Waiting for ingress-nginx controller pod to be ready..."
  kubectl rollout status deployment/ingress-nginx-controller \
    -n $NAMESPACE-ingress \
    --timeout=3m

}

function install_ingress_nginx() {
  echo "(Re-)Installing ingress-nginx..."
  helm repo add ingress-nginx-repo https://kubernetes.github.io/ingress-nginx
  helm repo update

  local base_args=(
    --namespace $NAMESPACE-ingress
    --create-namespace
    --set controller.replicaCount=1
    --set controller.podDisruptionBudget.enabled=true
    --set controller.podDisruptionBudget.minAvailable=1
  )

  if [ "$IS_REMOTE" = true ]; then
    helm upgrade --install ingress-nginx ingress-nginx-repo/ingress-nginx \
      "${base_args[@]}" \
      --set controller.resources.requests.cpu="$INGRESS_REMOTE_CPU" \
      --set controller.resources.requests.memory="$INGRESS_REMOTE_MEMORY" \
      --set controller.resources.limits.cpu="$INGRESS_REMOTE_CPU" \
      --set controller.resources.limits.memory="$INGRESS_REMOTE_MEMORY" \
      --set controller.nodeSelector.dedicated=ingress \
      --set controller.ingressClass=nginx \
      --set controller.service.type=NodePort \
      --set controller.service.nodePorts.http="$EC2_PORT" \
      --set controller.service.nodePorts.https="$EC2_PORT_SSL"

    wait_for_ingress_controller

    . ./ip.sh
    export PUBLIC_IP="${PUBLIC_IP}.nip.io"
  else
    helm upgrade --install ingress-nginx ingress-nginx-repo/ingress-nginx \
      "${base_args[@]}" \
      --set controller.resources.requests.cpu="$INGRESS_CPU" \
      --set controller.resources.requests.memory="$INGRESS_MEMORY" \
      --set controller.resources.limits.cpu="$INGRESS_CPU" \
      --set controller.resources.limits.memory="$INGRESS_MEMORY"

    wait_for_ingress_controller

    export PUBLIC_IP=localhost
  fi

  kubectl patch ingressclass nginx \
    -p '{"metadata":{"annotations":{"ingressclass.kubernetes.io/is-default-class":"true"}}}'
}

install_ingress_nginx

popd >/dev/null
