#!/bin/bash

set -e
pushd $(dirname "$0") > /dev/null

# Parse arguments
IS_REMOTE=false
for arg in "$@"; do
  if [ "$arg" = "-remote" ]; then
    IS_REMOTE=true
  fi
done

. ./.env
./test.sh

echo "Setting up AWS credentials..."
kubectl delete secret cloudthrash-secrets || true
kubectl create secret generic cloudthrash-secrets --from-env-file=.env

./install-external-charts.sh

# Set full image repo URL if remote, else leave it short
if [ "$IS_REMOTE" = true ]; then
  IMAGE_REPO_PREFIX="533267369548.dkr.ecr.us-east-1.amazonaws.com/"
else
  IMAGE_REPO_PREFIX=""
fi

helm upgrade --install cloud-thrash . \
  -f values.yaml \
  --set deployments.backend.env.MINIO_ACCESS_KEY="$USERNAME_TOOLS" \
  --set deployments.backend.env.MINIO_SECRET_KEY="$PASSWORD_TOOLS" \
  --set global.imageRepoPrefix="$IMAGE_REPO_PREFIX"

echo "integration test running..."
kubectl get pods -n default --no-headers | grep -vE 'test-' | awk '{print $1}' | \
    xargs -I {} kubectl wait --for=condition=ready pod {} -n default --timeout=300s

for i in {1..10}; do
    echo "Attempt $i/10 ..."
    sleep 10
    if helm test cloud-thrash; then
        break
    fi
done

kubectl delete pod -l helm.sh/hook=test
echo "integration test passed."

popd > /dev/null
