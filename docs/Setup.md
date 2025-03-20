# CloudThrash Setup Guide

This guide explains how to build the Docker images for CloudThrash, set up a local Kubernetes cluster, and prepare the frontend for local development. The goal is to establish a simple and efficient development workflow.

---

## Building Docker Images

CloudThrash consists of multiple containerized services running in a Kubernetes cluster. Each service requires its own Docker image.

### Prerequisites

1. **Install Docker Desktop**
   - Download and install [Docker Desktop](https://www.docker.com/products/docker-desktop). Docker Desktop includes an easy-to-use Kubernetes integration for local development.

2. **Additional Dependencies**
   - Install NodeJS, Python, Helm, Visual Studio Code, and other required tools with the following command:
     ```shell
     scoop install nodejs python311 helm vscode eksctl aws
     ```

### Building the Docker Images

The Docker images are built using predefined scripts in the project directory. These scripts leverage `docker build` while adding necessary configurations.

#### Steps to Build Images

1. Open a **Bash shell** (e.g., in Visual Studio Code).
2. Navigate to the project root directory.
3. Run the following script to build all images at once:
   ```shell
   ./infrastructure/docker/build-images/build-images-for-all.sh
   ```
4. Verify the created images with:
   ```shell
   docker images
   ```

---

## Setting Up a Kubernetes Cluster

A Kubernetes cluster is required to run CloudThrash. For local development, Docker Desktop is recommended as it comes with built-in Kubernetes support.

### Prerequisites

- **Enable Kubernetes in Docker Desktop**
  Go to Docker Desktop: `Settings -> Kubernetes` and enable Kubernetes.

- **Provide Secrets**
  1. Open KeePass and locate the entry `CloudThrash`.
  2. Download the `.env` file from the `Attachments` section.
  3. Copy the file to `infrastructure/helm/.env`.

### Starting the Cluster

1. **Ensure all required Docker images are available.**
   If needed, re-run the image build script.

2. **Start the Kubernetes cluster:**
   Run the following script to start the cluster:
   ```shell
   ./infrastructure/helm/install.sh
   ```
   Alternatively, use the setup script that combines all required steps:
   ```shell
   ./infrastructure/docker/setup-local.sh
   ```

---

## Resetting or Stopping the Kubernetes Cluster

- **Reset the cluster:**
  Go to Docker Desktop: `Settings -> Kubernetes -> Reset Kubernetes Cluster`.

- **Stop the cluster:**
  Close Docker Desktop to stop the cluster.

---

## Local Frontend Development

To test frontend changes without rebuilding Docker images, you can run the frontend locally while connecting to the backend in the Kubernetes cluster.

### Steps for Local Frontend Development

1. **Start the cluster:**
   Start the Kubernetes cluster as described above.

2. **Configure the frontend:**
   Create a `.env.local` file in the frontend directory with the following content:
   ```plaintext
   REACT_APP_CLOUDTHRASH_API_HOST=http://localhost
   ```

3. **Start the frontend locally:**
   Run the following command in the frontend directory:
   ```shell
   npm start --watch
   ```
   A browser should open automatically, and the frontend will be ready. It will connect to the backend running in the Kubernetes cluster.

---

## Tests  

The application is equipped with a comprehensive testing strategy to ensure quality, stability, and security. The tests cover various aspects, from static code analysis to full end-to-end testing.  

### Static Code Analysis  

To maintain clean and maintainable code, **Sonar** is used for static code analysis. This helps detect potential errors, security vulnerabilities, and violations of coding standards at an early stage.  

Additionally, **Hadolint** is employed for linting Dockerfiles, ensuring best practices are followed and reducing the risk of security issues or inefficiencies in containerized environments.  

For dependency management, **Dependabot** is integrated to automatically check for outdated or vulnerable dependencies, keeping the project secure and up to date.  

### Unit Tests  

To ensure the correctness of individual components and services, the application employs:  

- **Spring Boot Test** for backend logic  
- **Jasmine** for frontend unit testing  

These tests help verify that functions behave as expected in isolation, without interference from external dependencies.  

### Integration Tests  

Integration tests verify that different services work correctly together within the cluster. These tests are executed as **pods** inside the Kubernetes environment and can be triggered using the following command:  

```bash
helm test
```  

Running tests in the actual cluster environment ensures that all dependencies are properly considered and real-world scenarios are accurately simulated.  

### End-to-End Tests  

To validate the entire application workflow—from frontend interactions to backend processing and database operations—**End-to-End (E2E) tests** are implemented using **Playwright**. These tests simulate real user interactions with the UI to ensure all components function seamlessly together.  

The frontend E2E tests are located in the `services/frontend-test` directory and can be executed with the following commands:  

```bash
npm install
npx playwright test --headed
```  

Running the tests in **headed mode** allows for real-time observation of the UI in a browser window, making debugging and visual validation easier.  
