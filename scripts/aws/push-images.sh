#!/bin/bash
# Description: Push Docker images to AWS ECR (implicitly done with create-cluster).

pushd "$(dirname "$0")" >/dev/null
. ./env.sh

function build_images() {
  ../docker/build-all.sh
}

function login_ecr() {
  echo "Logging into AWS ECR repository: $ECR_REPOSITORY"
  aws ecr get-login-password --region "$AWS_DEFAULT_REGION" |
    docker login --username AWS --password-stdin "$ECR_REPOSITORY"
}

function push_images() {
  docker images --format "{{.Repository}}:{{.Tag}}" | grep "^$APP_NAME/" | while read -r image; do
    image_name="${image%%:*}"
    image_tag="${image##*:}"
    target_image="$ECR_REPOSITORY/$image_name:$image_tag"

    if aws ecr describe-repositories --repository-names "$image_name" >/dev/null 2>&1; then
      echo "Repository $image_name already exists. Skipping creation."
    else
      aws ecr create-repository --repository-name "$image_name"
      echo "Repository $image_name created."
    fi

    echo "Tagging $image as $target_image"
    docker tag "$image" "$target_image"

    echo "Pushing $target_image"
    docker push "$target_image"
  done
}

build_images
login_ecr
push_images

popd >/dev/null
