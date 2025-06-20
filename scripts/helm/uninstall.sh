#!/bin/bash
# Description: [-remote] Uninstall all Helm charts (local or remote cluster).

IS_REMOTE=false
for arg in "$@"; do
    case "$arg" in
    -remote) IS_REMOTE=true ;;
    esac
done

pushd "$(dirname "$0")" >/dev/null

if [ "$IS_REMOTE" = true ]; then
    . ../aws/connect-cluster.sh
fi

helm uninstall $APP_NAME --namespace "$APP_NAME"
kubectl delete namespace "$APP_NAME"

helm uninstall ingress-nginx --namespace "$APP_NAME-ingress"
kubectl delete namespace "$APP_NAME-ingress"

popd >/dev/null
