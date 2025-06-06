#!/bin/bash

pushd $(dirname $0) > /dev/null

# Parse arguments
IS_REMOTE=false
for arg in "$@"; do
  if [ "$arg" = "-remote" ]; then
    IS_REMOTE=true
  fi
done

if [ "$IS_REMOTE" = true ]; then
  helm uninstall ingress-nginx -n ingress-ec2
else
  helm uninstall ingress-nginx
fi

helm uninstall minio influxdb grafana thrash-buddy

popd > /dev/null
