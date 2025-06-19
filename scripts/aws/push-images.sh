#!/bin/bash
# Description: Push Docker images to an AWS container registry (implicitly done with create-cluster).

pushd "$(dirname "$0")" >/dev/null

../docker/build-all.sh
. ./env.sh

aws ecr get-login-password --region $AWS_DEFAULT_REGION | docker login --username AWS --password-stdin "$ECR_REPOSITORY"

docker images --format "{{.Repository}}:{{.Tag}}" | grep ^thrash-buddy/ | while read -r image; do
  image_name=$(echo "$image" | cut -d':' -f1)
  image_tag=$(echo "$image" | cut -d':' -f2)
  repository_name=$image_name
  target_image="$ECR_REPOSITORY/$repository_name:$image_tag"

  if aws ecr describe-repositories --repository-names "$repository_name" >/dev/null 2>&1; then
    echo "Repository $repository_name already exists. Skipping creation."
  else
    aws ecr create-repository --repository-name "$repository_name"
    echo "Repository $repository_name created."
  fi

  echo "Tagging $image as $target_image"
  docker tag "$image" "$target_image"

  echo "Pushing $target_image"
  docker push "$target_image"
done

popd >/dev/null
