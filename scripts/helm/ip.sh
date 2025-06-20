#!/bin/bash

set -e

pushd "$(dirname "$0")" >/dev/null

echo "Fetching public IP of Ingress EC2 node..."

EC2_PRIVATE_IP=$(kubectl get node -l dedicated=ingress -o jsonpath="{.items[0].status.addresses[?(@.type=='InternalIP')].address}")
PUBLIC_IP=$(aws ec2 describe-instances \
  --filters "Name=private-ip-address,Values=${EC2_PRIVATE_IP}" \
  --query "Reservations[].Instances[].PublicIpAddress" \
  --output text)

if [[ -z "$PUBLIC_IP" ]]; then
  echo "Could not fetch EC2 public IP"
  exit 1
fi

echo "Detected EC2 Public IP: $PUBLIC_IP"

popd >/dev/null
