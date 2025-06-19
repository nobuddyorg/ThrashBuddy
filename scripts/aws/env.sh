pushd "$(dirname "$0")" >/dev/null

. ../../configs/global.conf

EKS_ENVIRONMENT_NAME=${EKS_ENVIRONMENT_NAME:-stage}
EKS_CLUSTER_NAME="${APP_NAME}-${EKS_ENVIRONMENT_NAME}"

AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
AWS_DEFAULT_REGION=$(aws configure get region)
AWS_DEFAULT_REGION=${AWS_DEFAULT_REGION:-e-central-1}

ECR_REPOSITORY="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com"

popd >/dev/null
