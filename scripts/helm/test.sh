#!/bin/bash
# Description: Validate the Helm chart, including linting, templating, and unit tests.

set -e

pushd "$(dirname "$0")" >/dev/null

. ../setup/get-config.sh
./dependencies.sh

echo "Validating Helm chart..."

KUBE_SCORE_BINARY="$(pwd)/.deps/kube-score"
KUBE_CONFORM_BINARY="$(pwd)/.deps/kubeconform"

[ -x "$KUBE_SCORE_BINARY" ] || {
    echo "Missing kube-score binary"
    exit 1
}
[ -x "$KUBE_CONFORM_BINARY" ] || {
    echo "Missing kubeconform binary"
    exit 1
}

pushd ../../configs/helm >/dev/null

helm lint .
TEMPLATE_FILES=$(find templates -type f -name '*.yaml' -exec echo --show-only {} \;)
TEMPLATE_OUTPUT=$(helm template . $TEMPLATE_FILES)
echo "$TEMPLATE_OUTPUT" | "$KUBE_CONFORM_BINARY" -strict -verbose
echo "$TEMPLATE_OUTPUT" | "$KUBE_SCORE_BINARY" score -
helm install $APP_NAME --dry-run --debug .
echo "Helm chart is valid."

echo "Unit testing Helm chart..."
helm plugin install https://github.com/quintush/helm-unittest || true
helm plugin update unittest
helm unittest . --strict
echo "Helm chart unit tests passed."

popd >/dev/null
popd >/dev/null
