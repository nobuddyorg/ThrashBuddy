#!/bin/bash

set -e

pushd "$(dirname "$0")" >/dev/null

KUBE_SCORE_VERSION="1.19.0"
KUBECONFORM_VERSION="0.6.7"

KUBE_SCORE_BASE_URL="https://github.com/zegl/kube-score/releases/download/v${KUBE_SCORE_VERSION}"
KUBECONFORM_BASE_URL="https://github.com/yannh/kubeconform/releases/download/v${KUBECONFORM_VERSION}"

OS=$(uname -s | tr '[:upper:]' '[:lower:]')
ARCH=$(uname -m)
if [ "$ARCH" != "x86_64" ]; then
  echo "Error: This script only supports x86_64 architecture."
  exit 1
fi

if [[ "$OS" == "mingw"* || "$OS" == "cygwin"* ]]; then
  OS="windows"
fi

KUBE_SCORE_TARGET="kube-score"
KUBECONFORM_TARGET="kubeconform"
if [ "$OS" == "linux" ]; then
  KUBE_SCORE_BINARY="kube-score_${KUBE_SCORE_VERSION}_linux_amd64.tar.gz"
  KUBECONFORM_BINARY="kubeconform-linux-amd64.tar.gz"
elif [ "$OS" == "darwin" ]; then
  KUBE_SCORE_BINARY="kube-score_${KUBE_SCORE_VERSION}_darwin_amd64.tar.gz"
  KUBECONFORM_BINARY="kubeconform-darwin-amd64.tar.gz"
elif [ "$OS" == "windows" ]; then
  KUBE_SCORE_BINARY="kube-score_${KUBE_SCORE_VERSION}_windows_amd64.exe"
  KUBECONFORM_BINARY="kubeconform-windows-amd64.zip"
else
  echo "Error: Unsupported OS: $OS"
  exit 1
fi

DOWNLOAD_DIR="./.deps"
mkdir -p "$DOWNLOAD_DIR"

download_binary() {
  local base_url=$1
  local binary_name=$2
  local target_name=$3

  echo "Downloading $target_name..."
  curl -L "${base_url}/${binary_name}" -o "${DOWNLOAD_DIR}/${binary_name}"

  if [ ! -s "${DOWNLOAD_DIR}/${binary_name}" ] || grep -q "Not Found" "${DOWNLOAD_DIR}/${binary_name}"; then
    echo "Error: Failed to download $target_name. Please check the URL."
    exit 1
  fi

  if [[ "$binary_name" == *.tar.gz ]]; then
    tar -xzf "${DOWNLOAD_DIR}/${binary_name}" -C "$DOWNLOAD_DIR"
    chmod +x "${DOWNLOAD_DIR}/${target_name}"
  elif [[ "$binary_name" == *.zip ]]; then
    unzip -o "${DOWNLOAD_DIR}/${binary_name}" -d "$DOWNLOAD_DIR"
    chmod +x "${DOWNLOAD_DIR}/${target_name}"
  else
    mv "${DOWNLOAD_DIR}/${binary_name}" "${DOWNLOAD_DIR}/${target_name}"
    chmod +x "${DOWNLOAD_DIR}/${target_name}"
  fi

  echo "$target_name is available at: ${DOWNLOAD_DIR}/${target_name}"
}

download_binary "$KUBE_SCORE_BASE_URL" "$KUBE_SCORE_BINARY" "$KUBE_SCORE_TARGET"
download_binary "$KUBECONFORM_BASE_URL" "$KUBECONFORM_BINARY" "$KUBECONFORM_TARGET"

echo "All binaries are downloaded and ready to use in ${DOWNLOAD_DIR}."

popd >/dev/null
