#!/bin/bash

pushd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null

set -a
. ../../configs/.env
. ../../configs/global.conf
set +a

popd >/dev/null
