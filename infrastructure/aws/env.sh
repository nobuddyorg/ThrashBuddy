export ENVIRONMENT=${ENVIRONMENT:-stage}
export CLUSTER_NAME="cloud-thrash"
export AWS_REGION=$(aws configure get region)
export AWS_REGION=${AWS_REGION:-eu-central-1}
export CLUSTER_NAME="${CLUSTER_NAME}-${ENVIRONMENT}"
