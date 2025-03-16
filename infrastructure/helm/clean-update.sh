#!/bin/bash

pushd $(dirname $0) > /dev/null

./uninstall.sh
../docker/build-all.sh
./install.sh

popd > /dev/null
