#!/bin/bash

pushd $(dirname $0) > /dev/null

../docker/build-all.sh

# Get AWS details
. ./env

# Login to ECR
aws sts get-caller-identity --query Account --output text
aws ecr get-login-password --region $AWS_DEFAULT_REGION | docker login --username AWS --password-stdin "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com"

# Push all images starting with cloud-thrash/ into separate repositories
docker images --format "{{.Repository}}:{{.Tag}}" | grep ^cloud-thrash/ | while read -r image; do
  image_name=$(echo "$image" | cut -d':' -f1)
  image_tag=$(echo "$image" | cut -d':' -f2)
  repository_name=$image_name
  target_image="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$repository_name:$image_tag"

  # Create repository if it doesn't exist
  if aws ecr describe-repositories --repository-names "$repository_name" > /dev/null 2>&1; then
      echo "Repository $repository_name already exists. Skipping creation."
  else
      aws ecr create-repository --repository-name "$repository_name"
      echo "Repository $repository_name created."
  fi

  # Tag and push the image
  echo "Tagging $image as $target_image"
  docker tag "$image" "$target_image"

  echo "Pushing $target_image"
  docker push "$target_image"
done

popd > /dev/null
