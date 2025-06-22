#!/bin/bash
# Description: Delete all Docker images and ECR repositories related to the application.

pushd "$(dirname "$0")" >/dev/null
. ./env.sh

docker images --format "{{.Repository}}:{{.Tag}}" | grep ^$APP_NAME/ | while read -r image; do
  docker rmi "$image" --force
done

aws ecr describe-repositories --query 'repositories[*].repositoryName' --output text | tr '\t' '\n' | grep ^$APP_NAME/ | while read -r repo; do
  echo "Deleting ECR repo: $repo"
  aws ecr delete-repository --repository-name "$repo" --force
done

docker system prune -af

popd >/dev/null
