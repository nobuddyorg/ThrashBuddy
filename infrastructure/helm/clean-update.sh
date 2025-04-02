#!/bin/bash

set -e
pushd $(dirname $0) > /dev/null

IS_REMOTE=false
for arg in "$@"; do
  if [ "$arg" = "-remote" ]; then
    IS_REMOTE=true
  fi
done

./uninstall.sh "$@"
../docker/build-all.sh

if [ "$IS_REMOTE" = true ]; then
  ../aws/push-images.sh
fi

./install.sh "$@"

popd > /dev/null
