#!/bin/bash
# Description: Delete an existing AWS cluster.

pushd "$(dirname "$0")" >/dev/null

. ./env.sh

./connect-cluster.sh
../helm/uninstall.sh -remote

eksctl utils write-kubeconfig --cluster ${EKS_CLUSTER_NAME} && kubectl config use-context $(kubectl config get-contexts -o name | grep ${EKS_CLUSTER_NAME})
eksctl delete cluster --name $EKS_CLUSTER_NAME

popd >/dev/null
