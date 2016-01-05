package com.miniplay.minicortex.server;

import com.miniplay.common.CommandExecutor;
import com.miniplay.common.Debugger;
import com.miniplay.minicortex.config.Config;
import com.miniplay.minicortex.config.ConfigManager;
import com.miniplay.minicortex.exceptions.DependenciesNotInstalled;
import com.miniplay.minicortex.lib.ClassHelpers;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;
import com.miniplay.minicortex.observers.AbstractObserver;
import com.miniplay.minicortex.observers.ObserverManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 *
 * Created by ret on 4/12/15.
 */
public class CortexServer {

    // Cortex config
    protected Config configInstance = null;

    // Executors
    public ScheduledExecutorService statusThreadPool = Executors.newScheduledThreadPool(2);

    // Modules
    private ElasticBalancer elasticBalancer = null;

    // Observers
    private ObserverManager observerManager = null;

    /**
     * CortexServer constructor
     */
    public CortexServer() throws DependenciesNotInstalled {

        this.checkDependencies();

        // Load app config
        this.configInstance = ConfigManager.getConfig();

        // Load Modules
        this.elasticBalancer = ElasticBalancer.getInstance();
        this.observerManager = new ObserverManager();

    }

    /**
     * Check if docker & docker-machine are installed, if not throw exception and exit.
     */
    private void checkDependencies() throws DependenciesNotInstalled {
        try {
            CommandExecutor.getInstance().execute("docker-machine help");
        }catch(IOException e) {
            throw new DependenciesNotInstalled("Package 'docker-machine' is not installed, minicortex cannot run without it");
        }
    }

    /**
     * Let's run the CortexServer
     */
    public void run() throws Exception {
        // Check balancer config is OK
        if(!this.elasticBalancer.isLoaded) {
            throw new Exception("Elastic Balancer not loaded!");
        }

        // Provision containers if needed
        this.elasticBalancer.triggerProvisionContainers();

        // All OK, Run executors!
        Debugger.getInstance().printOutput(" Server started!");
        System.out.println("\n");
        this.runObserverRunnables();
    }

    /**
     * Observers the CortexServer is going to run periodically (AUTO-SETUP)
     */
    private void runObserverRunnables() {
        // Iterate through Custom Observers file
        ArrayList<String> observerNames = ClassHelpers.getClassNamesFromPackage(this.configInstance.CUSTOM_OBSERVERS_PACKAGE_NAME);
        System.out.println("CLASS NAMES IN PACKAGE: " + observerNames);
        for (String observerClassName : observerNames) {
            try{
                AbstractObserver loadedObserver = ClassHelpers.instantiateClass(this.configInstance.CUSTOM_OBSERVERS_PACKAGE_NAME + "." + observerClassName, AbstractObserver.class);
                observerManager.add(loadedObserver);
            }catch (Exception e) { /* Fail silently */}

        }

        observerManager.startRunnables();

    }
}
