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

eksctl create nodegroup -f ec2-nodegroup.yaml

VPC_ID=$(aws ec2 describe-vpcs --query "Vpcs[0].VpcId" --output text --region $AWS_DEFAULT_REGION)
SUBNET_IDS=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" --query "Subnets[*].SubnetId" --output text --region $AWS_DEFAULT_REGION)
SUBNET_IDS_LIST=$(echo $SUBNET_IDS | tr ' ' ',')
SECURITY_GROUP_IDS=$(aws ec2 describe-network-interfaces --filters "Name=subnet-id,Values=$SUBNET_IDS_LIST" --query "NetworkInterfaces[*].Groups[*].GroupId" --output text --region $AWS_DEFAULT_REGION | tr '\t' '\n' | sort -u)
for SG_ID in $SECURITY_GROUP_IDS; do
  echo "Authorizing security group ingress for $SG_ID on port 30080..."
  aws ec2 authorize-security-group-ingress \
    --group-id $SG_ID \
    --protocol tcp \
    --port 30080 \
    --cidr 0.0.0.0/0 \
    --region $AWS_DEFAULT_REGION || echo "Rule for port 30080 might already exist for $SG_ID"
done

../helm/install.sh -remote
./connect.sh

popd > /dev/null
