#!/bin/bash

pushd $(dirname $0) > /dev/null

helm uninstall ingress-nginx minio influxdb grafana cloud-thrash

popd > /dev/null
