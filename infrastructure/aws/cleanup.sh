#!/bin/bash

pushd $(dirname $0) > /dev/null

docker images --format "{{.Repository}}:{{.Tag}}" | grep ^thrash-buddy/ | while read -r image; do
  docker rmi "$image" --force
done

. ./env.sh
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# List and delete ECR repos starting with thrash-buddy/
aws ecr describe-repositories --query 'repositories[*].repositoryName' --output text | tr '\t' '\n' | grep ^thrash-buddy/ | while read -r repo; do
  echo "Deleting ECR repo: $repo"
  aws ecr delete-repository --repository-name "$repo" --force
done

docker system prune -af

popd > /dev/null
