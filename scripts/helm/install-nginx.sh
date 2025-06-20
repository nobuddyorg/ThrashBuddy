#!/bin/bash

echo "(Re-)Installing ingress-nginx..."
helm repo add ingress-nginx-repo https://kubernetes.github.io/ingress-nginx
helm repo update

if [ "$IS_REMOTE" = true ]; then
  helm upgrade --install ingress-nginx ingress-nginx-repo/ingress-nginx \
    --set controller.resources.requests.cpu=800m \
    --set controller.resources.requests.memory=800Mi \
    --set controller.resources.limits.cpu=800m \
    --set controller.resources.limits.memory=800Mi \
    --set controller.replicaCount=1 \
    --set controller.podDisruptionBudget.enabled=true \
    --set controller.podDisruptionBudget.minAvailable=1 \
    --set controller.nodeSelector.dedicated=ingress \
    --set controller.ingressClass=nginx \
    --set controller.service.type=NodePort \
    --set controller.service.nodePorts.http=30080 \
    --set controller.service.nodePorts.https=30443

  echo "Waiting for ingress-nginx controller pod..."
  kubectl wait --namespace default \
    --for=condition=ready pod \
    --selector=app.kubernetes.io/component=controller \
    --timeout=90s

  . ./ip.sh
  export PUBLIC_IP=${PUBLIC_IP}.nip.io
else
  helm upgrade --install ingress-nginx ingress-nginx-repo/ingress-nginx \
    --set controller.resources.requests.cpu=200m \
    --set controller.resources.requests.memory=192Mi \
    --set controller.resources.limits.cpu=200m \
    --set controller.resources.limits.memory=192Mi \
    --set controller.replicaCount=1 \
    --set controller.podDisruptionBudget.enabled=true \
    --set controller.podDisruptionBudget.minAvailable=1

  echo "Waiting for ingress-nginx controller pod..."
  kubectl wait --namespace default \
    --for=condition=ready pod \
    --selector=app.kubernetes.io/component=controller \
    --timeout=90s

  export PUBLIC_IP=localhost
fi

kubectl patch ingressclass nginx \
  -p '{"metadata":{"annotations":{"ingressclass.kubernetes.io/is-default-class":"true"}}}'
