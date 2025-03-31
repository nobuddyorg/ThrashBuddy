# Getting Started Guide

This guide explains how to build the Docker images for CloudThrash, set up a local Kubernetes cluster, and prepare the frontend for local development. The goal is to establish a simple and efficient development workflow.

## Prerequisites

-   AWS eksctl
-   Docker
-   Bash Shell (Git Bash on Windows is sufficient)
-   AWS credentials in your user home: `~/.aws/credentials` and `~/.aws/config`
-   Optional: Bruno for API

## Building Docker Images

CloudThrash consists of multiple containerized services running in a Kubernetes cluster. Each service requires its own Docker image.

### Prerequisites

1. **Install Docker Desktop**
   - Download and install [Docker Desktop](https://www.docker.com/products/docker-desktop). Docker Desktop includes an easy-to-use Kubernetes integration for local development. Activate the Kubernetes support in the Docker Desktop settings.

2. **Additional Dependencies**
   - Install Helm, Visual Studio Code, and other required tools with the following command:
     ```shell
     scoop install helm vscode eksctl aws
     ```
     or
     ```shell
     sudo apt update && sudo apt install -y helm code eksctl awscli
     ```

     Both the frontend and backend come with their own package managers and their wrappers `npmw` and `gradlew`. They will install themselves and their dependencies.

### Building the Docker Images

The Docker images are built using predefined scripts in the project directory. These scripts leverage `docker build` while adding necessary configurations.

#### Steps to Build Images

1. Start Docker Desktop (with Kubernetes enabled).
2. Open a **Bash shell**.
3. Navigate to the project root directory.
4. Run the following script to build all images at once:
   ```shell
   ./infrastructure/docker/build-all.sh
   ```
5. Verify the created images with:
   ```shell
   docker images | grep ^cloud-thrash/
   ```

## Setting Up a Kubernetes Cluster

A Kubernetes cluster is required to run CloudThrash. For local development, Docker Desktop is recommended as it comes with built-in Kubernetes support.

### Prerequisites

- **Enable Kubernetes in Docker Desktop**:
  Go to Docker Desktop: `Settings -> Kubernetes` and enable Kubernetes.

- **Provide Secrets**:
   Create a `infrastructure/helm/.env` file containing `USERNAME_TOOLS`, `PASSWORD_TOOLS` and `INFLUXDB_API_TOKEN` of your choice.

### Starting the Cluster

1. **Ensure all required Docker images are available.**
   If needed, re-run the image build script.

2. **install into Kubernetes cluster:**
   Run the following script to start the cluster:
   ```shell
   ./infrastructure/helm/install.sh
   ```

## Resetting or Stopping the Kubernetes Cluster

- **Reset the cluster:**
  Go to Docker Desktop: `Settings -> Kubernetes -> Reset Kubernetes Cluster`.

- **Stop the cluster:**
  Close Docker Desktop to stop the cluster.

## Local Development

To test frontend and backend changes without rebuilding Docker images, you can run the services locally.

### Steps for Local Development

1. **Start the backend:**
   ```shell
   cd services/backend
   ./gradlew bootRun
   ```

2. **Start the frontend locally:**
   Run the following command in the frontend directory:
   ```shell
   cd services/frontend
   ng serve --open
   ```
   A browser should open automatically, and the frontend will be ready. It will connect to the backend running in the Kubernetes cluster.

Backend and frontend should be connected automatically. This setup is a bit limited though, as Minio, InfluxDB and Grafana are not started. 

## Tests  

The application is equipped with a comprehensive testing strategy to ensure quality, stability, and security. The tests cover various aspects, from static code analysis to full end-to-end testing.  

### Static Code Analysis  

To maintain clean and maintainable code, **CodeQL** is used for static code analysis. This helps detect potential errors, security vulnerabilities, and violations of coding standards at an early stage.  

Additionally, **Hadolint** is utilized for linting Dockerfiles, ensuring best practices are followed and reducing the risk of security issues or inefficiencies in containerized environments.  

For dependency management, **Dependabot** is integrated to automatically check for outdated or vulnerable dependencies, keeping the project secure and up to date.  

Even Kubernetes files are checked with `kube-conform` and `kube-score`.

### Unit Tests  

To ensure the correctness of individual components and services, the application uses:  

- **Spring Boot Test** and **Spock** for backend logic  
- **Jasmine** for frontend unit testing  

These tests help verify that functions behave as expected in isolation, without interference from external dependencies.  

### Integration Tests  

Integration tests verify that different services work correctly together within the cluster. These tests are executed as **pods** inside the Kubernetes environment and can be triggered using the following command:  

```bash
helm test
```  

Running tests in the actual cluster environment ensures that all dependencies are properly considered.

### API Tests

With `Bruno` the API can be tested manually. It is not integrated in an automated way. The bruno files were used for developing the API. But API- and E2E-Tests would be almost identical, I decided against API-Test automation.

### End-to-End Tests  

To validate the entire application workflow — from frontend interactions to backend processing and InfluxDB operations — **End-to-End (E2E) tests** are implemented using **Playwright**. These tests simulate real user interactions with the UI to ensure all components function seamlessly together.  

The frontend E2E tests are located in the `services/frontend-test` directory and can be executed with the following commands:  

```bash
./npmw ci
./npmw run install
./npmw run headedTest
```  

Running the tests in **headed mode** allows for real-time observation of the UI in a browser window, making debugging and visual validation easier.  

## GitHub Self-Hosted Runner

_NOTE_: If everything runs smooth locally, you can setup a [self-hosted GitHub runner](github-runner.md) if you like.

## Cloud Setup

_NOTE_: If everything runs smooth locally, you can go on to the [Cloud Section](cloud.md).
