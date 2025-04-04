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

AZ=$(aws ec2 describe-subnets --region $AWS_DEFAULT_REGION \
    --filters "Name=tag:alpha.eksctl.io/cluster-name,Values=$CLUSTER_NAME" \
    --query "Subnets[*].AvailabilityZone" --output json | jq -r 'unique | .[0]')
cp ec2-nodegroup.yaml.template ec2-nodegroup.yaml
sed -i "s|\$AZ|$AZ|g" ec2-nodegroup.yaml
sed -i "s|\$AWS_DEFAULT_REGION|$AWS_DEFAULT_REGION|g" ec2-nodegroup.yaml
eksctl create nodegroup -f ec2-nodegroup.yaml

SECURITY_GROUP_IDS=$(aws ec2 describe-security-groups \
  --region $AWS_DEFAULT_REGION \
  --query "SecurityGroups[?Tags[?Key=='eksctl.cluster.k8s.io/v1alpha1/cluster-name' && Value=='$CLUSTER_NAME'] && Tags[?Key=='alpha.eksctl.io/nodegroup-name' && Value=='ingress-nginx-ec2']].GroupId" \
  --output text)
for SG_ID in $SECURITY_GROUP_IDS; do
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
