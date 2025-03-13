#!/bin/bash

script_dir=$(dirname "$(realpath "$0")")
pushd $(dirname $0)/../../../ > /dev/null

set -e
for script in $(ls $script_dir | grep -v $(basename $0)); do
    $script_dir/$script
done

popd > /dev/null
