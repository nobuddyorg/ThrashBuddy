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
# Immutable per-build tag (used instead of :latest for images built by this repo) so
# imagePullPolicy: IfNotPresent never mistakes a stale cached layer for a fresh deploy.
# Overridable by pre-exporting IMAGE_TAG (e.g. from a CI pipeline building a specific commit).
IMAGE_TAG="${IMAGE_TAG:-$(git -C "$(pwd)" rev-parse --short HEAD 2>/dev/null || date +%Y%m%d%H%M%S)}"
set +a

popd >/dev/null
