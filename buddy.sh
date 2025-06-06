#!/bin/bash

pushd $(dirname $0) > /dev/null

if [ "$#" -lt 2 ]; then
  echo -e "
Usage: $0 <command-group> <command> [options]

Available command groups:

  aws:
    connect             - Establish a connection to AWS EKS cluster (if created).
    cleanup             - Remove persistent resources from AWS (mainly S3 buckets and contents).
    create-cluster      - Create a new AWS cluster.
    delete-cluster      - Delete an existing AWS cluster.
    push-images         - Push Docker images to an AWS container registry (implicitly done with create-cluster).

  docker:
    build-all           - Build all Docker images required for the project (implicitly done with create-cluster).
    build-<image>       - Build a specific Docker image (e.g., backend, frontend, k6).

  helm:
    install [-remote]   - Install all Helm charts, including thrash-buddy itself (local or remote cluster).
    uninstall [-remote] - Uninstall all Helm charts (local or remote cluster).
    update [-remote]    - Update the Helm chart locally or remotely if '-remote' is specified (local or remote cluster).
"
  exit 1
fi

BASE_DIR=$1
COMMAND=$2
OPTION=$3

pushd infrastructure/$BASE_DIR > /dev/null

./$COMMAND.sh $OPTION

popd > /dev/null
popd > /dev/null
