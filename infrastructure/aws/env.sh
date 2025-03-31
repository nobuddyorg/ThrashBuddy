export ENVIRONMENT=${ENVIRONMENT:-stage}
export CLUSTER_NAME="cloud-thrash"
export AWS_DEFAULT_REGION=$(aws configure get region)
export AWS_DEFAULT_REGION=${AWS_DEFAULT_REGION:-eu-central-1}
export CLUSTER_NAME="${CLUSTER_NAME}-${ENVIRONMENT}"
