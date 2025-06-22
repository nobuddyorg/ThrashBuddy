#!/bin/bash

set -e
set -o pipefail

# Allow passwordless sudo for current user (if not already configured)
if [ ! -f "/etc/sudoers.d/$(whoami)" ]; then
  echo "$(whoami) ALL=(ALL) NOPASSWD:ALL" | sudo tee "/etc/sudoers.d/$(whoami)" >/dev/null
  sudo chmod 0440 "/etc/sudoers.d/$(whoami)"
fi

# Update and upgrade system packages
sudo apt update
sudo apt upgrade -y

# Install essential build tools and Docker
sudo apt install -y build-essential curl file git docker.io

# Add current user to Docker group
sudo usermod -aG docker $(whoami)

# Install Homebrew (Linuxbrew)
if ! command -v brew &>/dev/null; then
  /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
fi

# Set up Homebrew environment
echo 'eval "$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)"' >>~/.profile
source ~/.profile
brew --version

# Install tools via Homebrew
brew install helm kubectl node minikube eksctl awscli

# Enable and start Docker via systemd
sudo systemctl enable docker
sudo systemctl start docker

# Run Docker test in a subshell with updated group
echo "ℹ️  Switching to a subshell with Docker group applied to verify access..."
newgrp docker <<EONG
echo "✅ Docker is now usable without sudo:"
docker version || echo "⚠️  Docker not working yet — try logging out and back in."
EONG

echo "✅ Setup complete!"
echo "ℹ️  If you still get permission errors with Docker, log out and back in (or run 'newgrp docker')."
