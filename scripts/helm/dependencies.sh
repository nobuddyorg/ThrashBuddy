#!/bin/bash
set -e

pushd "$(dirname "$0")" >/dev/null

KUBE_SCORE_VERSION="1.20.0"
KUBECONFORM_VERSION="0.7.0"

KUBE_SCORE_BASE_URL="https://github.com/zegl/kube-score/releases/download/v${KUBE_SCORE_VERSION}"
KUBECONFORM_BASE_URL="https://github.com/yannh/kubeconform/releases/download/v${KUBECONFORM_VERSION}"

DOWNLOAD_DIR="./.deps"

detect_os_arch() {
  local os=$(uname -s | tr '[:upper:]' '[:lower:]')
  local arch=$(uname -m)

  if [ "$arch" != "x86_64" ]; then
    echo "Error: Only x86_64 architecture is supported."
    exit 1
  fi

  if [[ "$os" == mingw* || "$os" == cygwin* ]]; then
    os="windows"
  fi

  echo "$os" "$arch"
}

prepare_download_dir() {
  if [ -d "$DOWNLOAD_DIR" ]; then
    rm -rf "$DOWNLOAD_DIR"
  fi
  mkdir -p "$DOWNLOAD_DIR"
}

get_binaries() {
  local os=$1

  case "$os" in
  linux)
    KUBE_SCORE_BINARY="kube-score_${KUBE_SCORE_VERSION}_linux_amd64.tar.gz"
    KUBECONFORM_BINARY="kubeconform-linux-amd64.tar.gz"
    ;;
  darwin)
    KUBE_SCORE_BINARY="kube-score_${KUBE_SCORE_VERSION}_darwin_amd64.tar.gz"
    KUBECONFORM_BINARY="kubeconform-darwin-amd64.tar.gz"
    ;;
  windows)
    KUBE_SCORE_BINARY="kube-score_${KUBE_SCORE_VERSION}_windows_amd64.exe"
    KUBECONFORM_BINARY="kubeconform-windows-amd64.zip"
    ;;
  *)
    echo "Error: Unsupported OS: $os"
    exit 1
    ;;
  esac

  KUBE_SCORE_TARGET="kube-score"
  KUBECONFORM_TARGET="kubeconform"
}

download_and_extract() {
  local base_url=$1
  local binary_name=$2
  local target_name=$3

  echo "Downloading $target_name..."
  curl -L "${base_url}/${binary_name}" -o "${DOWNLOAD_DIR}/${binary_name}"

  if [ ! -s "${DOWNLOAD_DIR}/${binary_name}" ] || grep -q "Not Found" "${DOWNLOAD_DIR}/${binary_name}"; then
    echo "Error: Failed to download $target_name. Please check the URL."
    exit 1
  fi

  case "$binary_name" in
  *.tar.gz)
    tar -xzf "${DOWNLOAD_DIR}/${binary_name}" -C "$DOWNLOAD_DIR"
    chmod +x "${DOWNLOAD_DIR}/${target_name}"
    ;;
  *.zip)
    unzip -o "${DOWNLOAD_DIR}/${binary_name}" -d "$DOWNLOAD_DIR"
    chmod +x "${DOWNLOAD_DIR}/${target_name}"
    ;;
  *)
    mv "${DOWNLOAD_DIR}/${binary_name}" "${DOWNLOAD_DIR}/${target_name}"
    chmod +x "${DOWNLOAD_DIR}/${target_name}"
    ;;
  esac

  echo "$target_name is available at: ${DOWNLOAD_DIR}/${target_name}"
}

read OS ARCH <<<"$(detect_os_arch)"
prepare_download_dir
get_binaries "$OS"

download_and_extract "$KUBE_SCORE_BASE_URL" "$KUBE_SCORE_BINARY" "$KUBE_SCORE_TARGET"
download_and_extract "$KUBECONFORM_BASE_URL" "$KUBECONFORM_BINARY" "$KUBECONFORM_TARGET"

echo "All binaries are downloaded and ready to use in ${DOWNLOAD_DIR}."

popd >/dev/null
