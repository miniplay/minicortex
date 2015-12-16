package com.miniplay.minicortex.modules.docker;

import com.miniplay.common.Debugger;
import com.miniplay.minicortex.config.ConfigManager;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Provision a container into a separate thread
 * so we can create multiple containers in the same time without blocking.
 * Created by ret on 16/12/15.
 */
public class ProvisionThread extends Thread {

    public Boolean isLastProvisionedMachine = false;

    public ProvisionThread (String s) {
        super(s);
    }

    public ProvisionThread (String s, Boolean isLastProvisionedMachine) {
        super(s);
        this.isLastProvisionedMachine = isLastProvisionedMachine;
    }

    public void run() {
        System.out.println("Running "+ getName());

        if(this.isLastProvisionedMachine) {
            Debugger.getInstance().debug("Provisioning last machine...",this.getClass());
        }

        try {
            // Generate secure random string
            SecureRandom random = new SecureRandom();
            String randomString =  new BigInteger(130, random).toString(32);

            // Provision container with random name
            String containerName = ConfigManager.getConfig().DOCKER_CONTAINER_HOSTNAME_BASENAME + randomString.substring(2,7);
            ElasticBalancer.getInstance().getContainerManager().provisionContainer(containerName,this.isLastProvisionedMachine);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return;

    }

}
