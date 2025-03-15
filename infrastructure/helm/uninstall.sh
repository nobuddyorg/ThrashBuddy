#!/bin/bash

pushd $(dirname $0) > /dev/null

helm uninstall ingress-nginx minio cloud-thrash

popd > /dev/null
