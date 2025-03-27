#!/bin/bash

pushd $(dirname $0) > /dev/null

. ./env

../helm/uninstall.sh

eksctl utils write-kubeconfig --cluster ${CLUSTER_NAME} && kubectl config use-context $(kubectl config get-contexts -o name | grep ${CLUSTER_NAME})
eksctl delete cluster --name $CLUSTER_NAME

popd > /dev/null
