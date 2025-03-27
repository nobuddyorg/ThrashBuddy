#!/bin/bash

pushd $(dirname $0) > /dev/null

. ./env.sh
eksctl utils write-kubeconfig --cluster ${CLUSTER_NAME} && kubectl config use-context $(kubectl config get-contexts -o name | grep ${CLUSTER_NAME})

kubectl port-forward deployment/ingress-nginx-controller 80:80 >/dev/null 2>&1 &

echo ""
echo -e "\e[1;34mVisit CloudThrash at: http://localhost:80/\e[0m"
echo ""

popd > /dev/null
