# Self-Hosted GitHub Runner  

This README describes the installation and configuration of a self-hosted GitHub Runner. This can be useful, because GitHub default runners might not have enough free working hours or too few resources. I usually use a NUC mini PC. But this process is completely optional.

_NOTE_: The runner must have Linux installed, because some GitHub actions only work on Linux. Preferable Ubuntu so that the installation script works.

## Installation and Configuration  

1. Access your machine (virtual or physical). 

2. Execute the setup script:  
   ```bash
   ./infrastructure/github-runner/prepare-runner.sh
   ```  

   The script installs all necessary dependencies, such as `curl`, `unzip`, and systemd components. Additionally, it configures permissions, allowing the user to run Docker without `sudo`. The GitHub Runner should be set up to start all required services automatically after a reboot. It is designed for Ubuntu runners. 

## Adding the Runner to GitHub  

1. Open the GitHub page of the respective repository or organization.  

2. Navigate to **Settings > Actions > Runners** and click **New self-hosted runner**.  

3. Follow the instructions to generate the configuration commands for the runner (do it on the runner).  

4. Copy the generated commands and execute them on your runner:  
   - The runner tool is downloaded.  
   - The runner is configured.  
   - The runner is started as a system service.  

5. Verify in GitHub that the runner appears as active.   
