#!/bin/bash
# Description: Remove persistent resources from AWS (mainly S3 buckets and contents).

pushd "$(dirname "$0")" >/dev/null

docker images --format "{{.Repository}}:{{.Tag}}" | grep ^thrash-buddy/ | while read -r image; do
  docker rmi "$image" --force
done

. ./env.sh

aws ecr describe-repositories --query 'repositories[*].repositoryName' --output text | tr '\t' '\n' | grep ^thrash-buddy/ | while read -r repo; do
  echo "Deleting ECR repo: $repo"
  aws ecr delete-repository --repository-name "$repo" --force
done

docker system prune -af

popd >/dev/null
