#!/bin/bash
# Description: [-remote] Uninstall all Helm charts (local or remote cluster).

pushd "$(dirname "$0")" >/dev/null

. ./parse-args.sh

if [ "$IS_REMOTE" = true ]; then
    . ../aws/connect-cluster.sh
fi

helm uninstall thrash-buddy

popd >/dev/null
