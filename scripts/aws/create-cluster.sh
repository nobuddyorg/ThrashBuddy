#!/bin/bash
# Description: Create a new AWS cluster.

pushd "$(dirname "$0")" >/dev/null
. ./env.sh

function create_cluster() {
  eksctl create cluster \
    --name "$EKS_CLUSTER_NAME" \
    --version "$EKS_KUBERNETES_VERSION" \
    --region "$AWS_DEFAULT_REGION" \
    --fargate

  eksctl create fargateprofile \
    --cluster $EKS_CLUSTER_NAME \
    --name $APP_NAME-profile \
    --namespace $NAMESPACE
}

function update_coredns_addon() {
  if [ -n "$EKS_CORE_DNS_VERSION" ]; then
    eksctl update addon \
      --name coredns \
      --version "$EKS_CORE_DNS_VERSION" \
      --cluster "$EKS_CLUSTER_NAME" \
      --force
  fi
}

function get_availability_zone() {
  aws ec2 describe-subnets --region "$AWS_DEFAULT_REGION" \
    --filters "Name=tag:alpha.eksctl.io/cluster-name,Values=$EKS_CLUSTER_NAME" \
    --query "Subnets[*].AvailabilityZone" --output json | jq -r 'unique | .[0]'
}

function create_nodegroup() {
  security_group_ids=$(aws ec2 describe-security-groups \
    --region $AWS_DEFAULT_REGION \
    --query "SecurityGroups[?Tags[?Key=='eksctl.cluster.k8s.io/v1alpha1/cluster-name' && Value=='$EKS_CLUSTER_NAME'] && Tags[?Key=='alpha.eksctl.io/nodegroup-name' && Value=='$APP_NAME-ingress']].GroupId" \
    --output text)

  for sg_id in $security_group_ids; do
    aws ec2 authorize-security-group-ingress \
      --group-id $sg_id \
      --protocol tcp \
      --port $EC2_PORT \
      --cidr 0.0.0.0/0 \
      --region $AWS_DEFAULT_REGION || echo "Rule for port $EC2_PORT might already exist for $sg_id"
  done
}

function install_helm() {
  ../helm/install.sh -remote
}

function connect() {
  ./connect-cluster.sh
}

create_cluster
update_coredns_addon
create_nodegroup
install_helm
connect

popd >/dev/null
