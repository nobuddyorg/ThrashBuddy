# Self-Hosted GitHub Runner  

This README describes the installation and configuration of a self-hosted GitHub Runner in our development infrastructure.  

---

## Installation and Configuration  

Access to the VM is only possible through the Proxmox web interface within the local company network.  

1. Log in to the Proxmox web interface (credentials can be found in [Confluence](https://qytera.atlassian.net/wiki/x/AYC4xQ)).  

2. Open the console of the corresponding VM (**GithubRunner**).  

3. Execute the setup script:  
   ```bash
   ./infrastructure/github-runner/prepare-runner.sh
   ```  

   The script installs all necessary dependencies, such as `curl`, `unzip`, and systemd components. Additionally, it configures permissions, allowing the user to run Docker without `sudo`. The GitHub Runner should be set up to start all required services automatically after a reboot.  

---

## Adding the Runner to GitHub  

1. Open the GitHub page of the respective repository or organization.  

2. Navigate to **Settings > Actions > Runners** and click **New self-hosted runner**.  

3. Follow the instructions to generate the configuration commands for the runner.  

4. Copy the generated commands and execute them in the Proxmox VM console:  
   - The runner tool is downloaded.  
   - The runner is configured.  
   - The runner is started as a system service.  

5. Verify in GitHub that the runner appears as active.  

---

## Environment Notes  

- The VM is only accessible via the Proxmox web interface within the local company network.  
- Details about the Proxmox configuration and access credentials can be found in our Confluence: [Confluence Link](https://qytera.atlassian.net/wiki/x/AYC4xQ).  
