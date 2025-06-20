#!/bin/bash
# Description: Create a new AWS cluster.

pushd "$(dirname "$0")" >/dev/null
. ./env.sh

function push_images() {
  ./push-images.sh
}

function create_cluster() {
  eksctl create cluster \
    --name "$EKS_CLUSTER_NAME" \
    --version "$EKS_KUBERNETES_VERSION" \
    --region "$AWS_DEFAULT_REGION" \
    --fargate
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

function prepare_nodegroup_config() {
  local az="$1"
  cp ../../configs/eks/template.ec2-nodegroup.yaml ec2-nodegroup.yaml
  sed -i "s|\$AZ|$az|g" ec2-nodegroup.yaml
  sed -i "s|\$AWS_DEFAULT_REGION|$AWS_DEFAULT_REGION|g" ec2-nodegroup.yaml
}

function create_nodegroup() {
  eksctl create nodegroup -f ec2-nodegroup.yaml
}

function get_security_group_ids() {
  aws ec2 describe-security-groups \
    --region "$AWS_DEFAULT_REGION" \
    --query "SecurityGroups[?Tags[?Key=='eksctl.cluster.k8s.io/v1alpha1/cluster-name' && Value=='$EKS_CLUSTER_NAME'] && Tags[?Key=='alpha.eksctl.io/nodegroup-name' && Value=='ingress-nginx-ec2']].GroupId" \
    --output text
}

function authorize_security_group_ingress() {
  local sg_id
  for sg_id in $(get_security_group_ids); do
    aws ec2 authorize-security-group-ingress \
      --group-id "$sg_id" \
      --protocol tcp \
      --port "$EC2_PORT" \
      --cidr 0.0.0.0/0 \
      --region "$AWS_DEFAULT_REGION" || echo "Rule for port $EC2_PORT might already exist for $sg_id"
  done
}

function install_helm_and_connect() {
  ../helm/install.sh -remote
  ./connect-cluster.sh
}

push_images
create_cluster
update_coredns_addon

availability_zone=$(get_availability_zone)
prepare_nodegroup_config "$availability_zone"
create_nodegroup

authorize_security_group_ingress
install_helm_and_connect

popd >/dev/null
