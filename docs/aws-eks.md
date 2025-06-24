# AWS - EKS (Elastic Kubernetes Service)

In AWS, we use a **Fargate-based EKS cluster** to run **ThrashBuddy**. This setup combines the benefits of a fully managed Kubernetes service with the flexibility of AWS Fargate for container orchestration.

## EKS with Fargate: Overview

AWS Fargate is a serverless compute engine for containers. When combined with EKS, it allows Kubernetes pods to run without managing worker nodes. Fargate automatically scales and ensures that each pod gets dedicated resources (CPU, RAM) according to the Kubernetes resource limits. Fargate has limited storage persistence. It does not work with EBS, but might require network storage solutions like EFS. that's why currently there is no permanent storage in place. For most use cases this won't be required anyway.

Only the ingress needs to run on a EC2 instance (`t3.small`), because Fargate instances in EKS can only run within private subnets and would not be reachable from internet.

## Prerequisites for EKS and Fargate

1. **Install AWS CLI**

   - Install the [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html)
   - and configure it with your access secrets

2. **Install eksctl**

   - `eksctl` is an open-source tool, now developed by AWS, that heavily relies on **CloudFormation**. It simplifies the management of EKS clusters.
   - Install `eksctl` with your preferred package manager

3. **Install kubectl**
   - You need `kubectl` to interact with Kubernetes. Install it.

Best use your favorite package manager like `scoop` or `homebrew`.

## EKSCTL

`eksctl` significantly simplifies the creation and management of EKS clusters. It automatically provisions **AWS CloudFormation stacks**, which define and manage the entire infrastructure as code (Infrastructure as Code, IaC).

AWS CloudFormation is a service that allows you to define and manage AWS resources like VPCs, subnets, security groups, IAM roles, and more in a structured YAML or JSON file. This declarative approach ensures seamless deployment, scaling, and management of infrastructure.

CloudFormation offers an advantage over Terraform as it is fully integrated with AWS and does not require separate state files. This makes resource creation and deletion easier while ensuring better visibility for AWS support in case of issues. It may create a vendor-lock, but the rest of the application runs within Kubernetes, it shouldn't be a big issue.

### Creating a Cluster with `eksctl`

A Fargate-based EKS cluster is created using a preconfigured script.

```shell
./buddy.sh aws create-cluster
```

This script sets up:

- The EKS control plane.
- Networking resources such as VPCs, subnets, and security groups.
- A Fargate profile that is automatically used for running pods.
- A node group for EC2 instances as well: the ingress runs there for public access

Once the cluster is created, the local Kubernetes context is switched accordingly. Even if a local Kubernetes cluster is running, `kubectl` will now connect to the AWS EKS cluster without requiring additional configuration. Running `kubectl get pods` will list all active pods in the EKS cluster, but no longer the local ones. This remains the case until the context is manually changed or the EKS cluster is deleted.

### Delete the Cluster

```shell
./buddy aws delete-cluster
```

After deletion, the local Kubernetes context is restored.
If you also want to get rid of the ECR repository and local docker images, you can run:

```shell
./buddy aws cleanup
```

### Pushing Docker Images to ECR

AWS EKS requires Docker images to be uploaded to Amazon Elastic Container Registry (ECR). A preconfigured script is available for this:

```shell
./buddy aws push-images
```

This script is automatically executed during cluster creation via `create-cluster`. However, it can also be run later to update Docker images. The script runs:

1. **Authenticate with ECR:**  
   The script handles authentication automatically.
2. **Build images:**  
   If images have not been built yet, they will be created.
3. **Push images:**  
   The images are uploaded to ECR so they can be used within the EKS cluster.

## Accessing ThrashBuddy

After cluster creation, **ThrashBuddy** is accessible via its ingress IP, printed in the console log after creation has finished.

If your Kubernetes context switched in the meantime, you can reconnect to the EKS cluster again by running:

```shell
./buddy aws connect-cluster
```

## AWS Configuration Notes

AWS requires a valid local configuration. This can either be stored in `~/.aws` or provided via environment variables.

Example using environment variables:

```shell
export AWS_ACCESS_KEY_ID=<your-access-key>
export AWS_SECRET_ACCESS_KEY=<your-secret-key>
export AWS_DEFAULT_REGION=<region>
```

Obviously the same secrets are also required for the GitHub Actions. The full list of secrets is:

- AWS_ACCESS_KEY_ID
- AWS_DEFAULT_REGION
- AWS_SECRET_ACCESS_KEY
- DOT_ENV (from /configs/.env)
