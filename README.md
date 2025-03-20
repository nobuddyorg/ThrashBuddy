# CloudThrash

<img src="docs/images/logo.jpg" alt="Logo" width="300" height="300">

**_Thrash your Web-App until it hurts._**

This repository provides a solution for distributed performance testing using k6, InfluxDB, and Grafana with AWS EKS. It leverages Docker for containerized environments and EksCtl for infrastructure as code.

## Features

-   **k6**: Used for load testing and performance benchmarking.
-   **InfluxDB**: Time-series database for storing performance metrics.
-   **Grafana**: Visualization tool to create dynamic dashboards for performance monitoring.
-   **Docker**: Containerized environments for easy deployment.
-   **AWS CLI**: Creating bootsrap components in AWS, like ECR for OCI-based images (e.g. Docker)
-   **EksCtl**: Infrastructure as Code (IaC) to automate the setup of cloud resources (using CloudFormation in the background).

Technology Map:

![](docs/images/technology-map.drawio.png)

## Getting Started

### Prerequisites

-   AWS EksCtl
-   Docker
-   Bash Shell (Git Bash on Windows is sufficient)
-   AWS credentials in your user home: `~/.aws/credentials` and `~/.aws/config`
-   Optional: Bruno for API

### Setup

1. **Clone the Repository**

    ```bash
    git clone git@github.com:besessener/CloudThrash.git
    cd CloudThrash
    ```

2. **Deploy**

    Execute `infrastructure/aws/create-cluster.sh` in the root to create a new AWS EKS cluster in your account. It will also create an SSL tunnel to your cluster so that you can access the application at http://localhost. It should be self explanatory with tooltips and stuff.

    <img src="docs/images/screenshot.png" alt="Screenshot" width="500" />

3. **Alternative API**

    You can also use the API directly, without the use of the Web-UI. The Bruno files should explain the usage.

## Architecture

This project leverages AWS EKS (Elastic Kubernetes Service) with Fargate instances to create load with k6 (JavaScript).

Architecture Diagram:

![](docs/images/architecture.drawio.png)

It is important to notice, the the entire app works serverless with Fargate instances. The amount of `vCPU` for Fargate is usually restricted and you might need to increase the quota according to your needs.

## Contributing

Contributions are welcome! Please open an issue or submit a pull request for any enhancements or bug fixes.

## License

This project is licensed under the MIT License.
