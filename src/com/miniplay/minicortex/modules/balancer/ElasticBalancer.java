package com.miniplay.minicortex.modules.balancer;

import com.miniplay.common.Debugger;
import com.miniplay.minicortex.config.Config;
import com.miniplay.minicortex.config.ConfigManager;
import com.miniplay.minicortex.modules.docker.ContainerManager;
import com.miniplay.minicortex.server.CortexServer;

import java.security.InvalidParameterException;
import java.util.Map;

/**
 * Cortex Elastic Balancer
 * Created by ret on 4/12/15.
 */
public class ElasticBalancer {

    /* Elastic Balancer Conf */
    public Boolean EB_ALLOW_PRIVISION_CONTAINERS = false;
    public Integer EB_MAX_PROVISION_CONTAINERS = 0;
    public Boolean isLoaded = false;

    /* Modules */
    private ContainerManager containerManager = null;


    /**
     * ElasticBalancer Instance
     */
    private static ElasticBalancer instance = null;

    /**
     * ElasticBalancer instance (Singleton)
     * @return ElasticBalancer instance
     */
    public static ElasticBalancer getInstance(){
        if(instance == null) {
            instance = new ElasticBalancer();
        }
        return instance;
    }

    /**
     * ElasticBalancer constructor
     */
    public ElasticBalancer() {

        // Load & validate config
        this.loadConfig();

        // Load Docker config
        this.containerManager = new ContainerManager(this);

        // All OK!
        this.isLoaded = true;
        Debugger.getInstance().printOutput("Elastic Balancer Loaded OK");
    }

    private void loadConfig() {
        Config config = ConfigManager.getConfig();
        if(config.EB_ALLOW_PRIVISION_CONTAINERS) {
            this.EB_ALLOW_PRIVISION_CONTAINERS = config.EB_ALLOW_PRIVISION_CONTAINERS;
        } else {
            throw new InvalidParameterException("EB_ALLOW_PRIVISION_CONTAINERS parameter does not exist");
        }

        if(config.EB_MAX_PROVISION_CONTAINERS > 0) {
            this.EB_MAX_PROVISION_CONTAINERS = config.EB_MAX_PROVISION_CONTAINERS;
        } else {
            throw new InvalidParameterException("EB_MAX_PROVISION_CONTAINERS parameter does not exist");
        }
    }

    public ContainerManager getContainerManager() {
        return containerManager;
    }
}
