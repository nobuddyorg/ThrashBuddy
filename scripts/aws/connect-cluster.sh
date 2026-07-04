#!/bin/bash
# Description: Establish a connection to AWS EKS cluster (if created).

set -e
set -o pipefail

pushd "$(dirname "$0")" >/dev/null

. ./env.sh
eksctl utils write-kubeconfig --cluster "$EKS_CLUSTER_NAME" && kubectl config use-context "$(kubectl config get-contexts -o name | grep "$EKS_CLUSTER_NAME")"

popd >/dev/null
