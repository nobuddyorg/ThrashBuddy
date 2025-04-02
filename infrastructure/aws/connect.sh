#!/bin/bash

pushd $(dirname $0) > /dev/null

. ./env.sh
eksctl utils write-kubeconfig --cluster ${CLUSTER_NAME} && kubectl config use-context $(kubectl config get-contexts -o name | grep ${CLUSTER_NAME})

popd > /dev/null
