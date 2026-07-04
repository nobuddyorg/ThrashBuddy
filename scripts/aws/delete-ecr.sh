#!/bin/bash
# Description: Delete all Docker images and ECR repositories related to the application.

set -e
set -o pipefail

pushd "$(dirname "$0")" >/dev/null
. ./env.sh

images=$(docker images --format "{{.Repository}}:{{.Tag}}" | grep "^$APP_NAME/" || true)
echo "$images" | while read -r image; do
  [ -z "$image" ] && continue
  docker rmi "$image" --force
done

repos=$(aws ecr describe-repositories --query 'repositories[*].repositoryName' --output text | tr '\t' '\n' | grep "^$APP_NAME/" || true)
echo "$repos" | while read -r repo; do
  [ -z "$repo" ] && continue
  echo "Deleting ECR repo: $repo"
  aws ecr delete-repository --repository-name "$repo" --force
done

docker system prune -af

popd >/dev/null
