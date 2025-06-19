#!/bin/bash
# Description: Validate the Helm chart, including linting, templating, and unit tests.

set -eu

pushd "$(dirname "$0")" >/dev/null

./dependencies.sh

echo "Validating Helm chart..."

KUBE_SCORE_BINARY="$(pwd)/.deps/kube-score"
KUBE_CONFORM_BINARY="$(pwd)/.deps/kubeconform"

[ -x "$KUBE_SCORE_BINARY" ] || { echo "Missing kube-score binary"; exit 1; }
[ -x "$KUBE_CONFORM_BINARY" ] || { echo "Missing kubeconform binary"; exit 1; }

pushd ../../charts >/dev/null

helm lint .
helm template . | "$KUBE_CONFORM_BINARY" -strict -verbose
helm template . | "$KUBE_SCORE_BINARY" score -
helm install thrash-buddy --dry-run --debug .
echo "Helm chart is valid."

echo "Unit testing Helm chart..."
helm plugin install https://github.com/quintush/helm-unittest || true
helm plugin update unittest
helm unittest . --strict
echo "Helm chart unit tests passed."

popd >/dev/null
popd >/dev/null
