package com.miniplay.minicortex.modules.docker;

import com.miniplay.common.Debugger;
import com.miniplay.minicortex.config.ConfigManager;
import com.miniplay.minicortex.exceptions.InvalidProvisionParams;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Provision a container into a separate thread
 * so we can create multiple containers in the same time without blocking.
 * Created by ret on 16/12/15.
 */
public class ProvisionThread extends Thread {

    public ProvisionThread (String s) {
        super(s);
    }

    public void run() {
        System.out.println("Running "+ getName());

        try {
            // Generate secure random string
            SecureRandom random = new SecureRandom();
            String randomString =  new BigInteger(130, random).toString(32);

            // Provision container with random name
            String containerName = ConfigManager.getConfig().DOCKER_CONTAINER_HOSTNAME_BASENAME + randomString.substring(2,7);
            ElasticBalancer.getInstance().getContainerManager().provisionContainer(containerName);

        } catch (InvalidProvisionParams e) {
            Debugger.getInstance().print("Error provisioning machine, caused by: " + e.getMessage(), this.getClass());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return;

    }

}
