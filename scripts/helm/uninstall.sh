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

. ../setup/get-config.sh

helm uninstall $APP_NAME --namespace $NAMESPACE || true
helm uninstall ingress-nginx --namespace $NAMESPACE || true
kubectl delete namespace $NAMESPACE --ignore-not-found || true

popd >/dev/null
