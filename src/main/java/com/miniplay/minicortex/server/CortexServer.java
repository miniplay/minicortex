package com.miniplay.minicortex.server;

import com.miniplay.common.Debugger;
import com.miniplay.minicortex.config.Config;
import com.miniplay.minicortex.config.ConfigManager;
import com.miniplay.minicortex.exceptions.DependenciesNotInstalled;
import com.miniplay.minicortex.lib.ClassHelpers;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;
import com.miniplay.minicortex.observers.AbstractObserver;
import com.miniplay.minicortex.observers.ObserverManager;

import java.util.ArrayList;

/**
 *
 * Created by ret on 4/12/15.
 */
public class CortexServer {

    // Cortex config
    protected Config configInstance = null;

    // Observers
    private ObserverManager observerManager = null;

    /**
     * CortexServer constructor
     */
    public CortexServer() throws DependenciesNotInstalled {
        // Load app config
        this.configInstance = ConfigManager.getConfig();
    }

    /**
     * Let's run the CortexServer
     */
    public void run() throws Exception {

        // Modules are loaded in run method, ElasticBalancer must only be instantiated from here!!
        ElasticBalancer elasticBalancer = ElasticBalancer.getInstance();
        this.observerManager = new ObserverManager();

        // Check balancer config is OK
        if(!elasticBalancer.isLoaded) {
            throw new Exception("Elastic Balancer not loaded!");
        }

        // Provision workers if needed
        elasticBalancer.triggerProvisionWorkers();

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
            if(this.configInstance.ENABLED_OBSERVERS.contains(observerClassName)) {
                try{
                    AbstractObserver loadedObserver = ClassHelpers.instantiateClass(this.configInstance.CUSTOM_OBSERVERS_PACKAGE_NAME + "." + observerClassName, AbstractObserver.class);
                    observerManager.add(loadedObserver);
                }catch (Exception e) { /* Fail silently */}
            } else {
                Debugger.getInstance().printOutput(" Observer ["+observerClassName+"] not scheduled. Reason: Not enabled in config");
            }


        }

        observerManager.startRunnables();

    }
}
