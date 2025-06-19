#!/bin/bash
# Description: Build all Docker images required for the project (implicitly done with create-cluster).

set -eu

script_dir=$(dirname "$(realpath "$0")")
pushd "$(dirname "$0")"/../../ >/dev/null

for script in $(ls $script_dir | grep -v $(basename $0)); do
    $script_dir/$script
done

popd >/dev/null
