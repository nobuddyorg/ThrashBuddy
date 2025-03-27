#!/bin/bash

# installs tools necessary for a self-hosted Github runner

echo "runner ALL=(ALL) NOPASSWD:ALL" | sudo tee -a /etc/sudoers > /dev/null

sudo apt update
sudo apt upgrade -y
sudo apt install -y build-essential curl file git docker.io

sudo usermod -aG docker runner

/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
echo 'eval "$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)"' >> ~/.profile
source ~/.profile
brew --version

brew install helm kubectl node minikube
