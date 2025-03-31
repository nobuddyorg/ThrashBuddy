#!/bin/bash

pushd $(dirname $0) > /dev/null

. ./env.sh

./push-images.sh

eksctl create cluster \
  --name $CLUSTER_NAME \
  --version 1.31 \
  --region $AWS_DEFAULT_REGION \
  --fargate

eksctl update addon --name coredns --version v1.11.4-eksbuild.2 --cluster $CLUSTER_NAME --force

../helm/install.sh
./connect.sh

popd > /dev/null
