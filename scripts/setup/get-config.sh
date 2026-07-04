#!/bin/bash

set -e

pushd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null

for f in ../../configs/.env ../../configs/global.conf; do
  if [ ! -f "$f" ]; then
    echo "Error: required config file '$f' not found. If this is configs/.env, run './buddy.sh helm install' to create it interactively; otherwise verify you're running from a checkout of the repository." >&2
    popd >/dev/null
    exit 1
  fi
done

set -a
. ../../configs/.env
. ../../configs/global.conf
set +a

popd >/dev/null
