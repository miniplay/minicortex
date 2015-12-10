package com.miniplay.minicortex.modules.balancer;

import com.miniplay.common.Utils;
import com.miniplay.minicortex.config.Config;
import com.miniplay.minicortex.modules.docker.ContainerManager;
import com.miniplay.minicortex.server.CortexServer;

import java.security.InvalidParameterException;
import java.util.Map;

/**
 * Cortex Elastic Balancer
 * Created by ret on 4/12/15.
 */
public class ElasticBalancer {

    public Map<String, Object> config = null;
    public Map<String, Object> dockerConfig = null;
    public Map<String, Object> amazonEC2Config = null;

    public Boolean EB_ALLOW_PRIVISION_CONTAINERS = false;
    public Integer EB_MAX_PROVISION_CONTAINERS = 0;

    private CortexServer cortexServer = null;
    private ContainerManager containerManager = null;
    public Boolean isLoaded = false;

    /**
     * ElasticBalancer constructor
     */
    public ElasticBalancer(CortexServer cortexServer, Config config) {

        // Load & validate config
        this.config = config.getElasticBalancerConfig();
        this.dockerConfig= config.getDockerConfig();
        this.amazonEC2Config= config.getAmazonEC2Config();
        this.loadConfig();

        // Load CortexServer
        this.cortexServer = cortexServer;

        // Load Docker config
        this.containerManager = new ContainerManager(this, dockerConfig, amazonEC2Config);

        // All OK!
        this.isLoaded = true;
        Utils.getInstance().printOutput("Elastic Balancer Loaded OK");
    }

    private void loadConfig() {
        if((Boolean)config.get("EB_ALLOW_PRIVISION_CONTAINERS")) {
            this.EB_ALLOW_PRIVISION_CONTAINERS = (Boolean) config.get("EB_ALLOW_PRIVISION_CONTAINERS");
        } else {
            throw new InvalidParameterException("EB_ALLOW_PRIVISION_CONTAINERS parameter does not exist");
        }

        if(config.get("EB_MAX_PROVISION_CONTAINERS") == (Integer)config.get("EB_MAX_PROVISION_CONTAINERS")) {
            this.EB_MAX_PROVISION_CONTAINERS = (Integer) config.get("EB_MAX_PROVISION_CONTAINERS");
        } else {
            throw new InvalidParameterException("EB_MAX_PROVISION_CONTAINERS parameter does not exist");
        }
    }

    public ContainerManager getContainerManager() {
        return containerManager;
    }
    public CortexServer getCortexServer() {
        return this.cortexServer;
    }
}