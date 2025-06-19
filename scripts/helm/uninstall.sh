#!/bin/bash
# Description: [-remote] Uninstall all Helm charts (local or remote cluster).

pushd "$(dirname "$0")" >/dev/null
pushd ../../charts >/dev/null

IS_REMOTE=false
for arg in "$@"; do
  case "$arg" in
  -remote) IS_REMOTE=true ;;
  esac
done

if [ "$IS_REMOTE" = "true" ]; then
  helm uninstall ingress-nginx -n ingress-ec2
else
  helm uninstall ingress-nginx
fi

helm uninstall minio influxdb grafana thrash-buddy

popd >/dev/null
popd >/dev/null
