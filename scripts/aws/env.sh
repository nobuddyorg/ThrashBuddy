#!/bin/bash

pushd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null

. ../setup/get-config.sh

export EKS_ENVIRONMENT_NAME=${EKS_ENVIRONMENT_NAME:-stage}
export EKS_CLUSTER_NAME="${APP_NAME}-${EKS_ENVIRONMENT_NAME}"

export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
export AWS_DEFAULT_REGION=$(aws configure get region)
if [ -z "$AWS_DEFAULT_REGION" ]; then
  export AWS_DEFAULT_REGION=eu-central-1
fi

export ECR_REPOSITORY="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com"

popd >/dev/null
