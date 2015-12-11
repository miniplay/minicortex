package com.miniplay.minicortex.server;

import com.miniplay.common.Debugger;
import com.miniplay.minicortex.config.Config;
import com.miniplay.minicortex.config.ConfigManager;
import com.miniplay.minicortex.lib.ClassHelpers;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;
import com.miniplay.minicortex.observers.AbstractObserver;
import com.miniplay.minicortex.observers.ObserverManager;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    public CortexServer() {

        // Load app config
        this.configInstance = ConfigManager.getConfig();

        // Load Modules
        this.elasticBalancer = ElasticBalancer.getInstance();
        this.observerManager = new ObserverManager();

    }

    /**
     * Let's run the CortexServer
     */
    public void run() throws Exception {
        // Check balancer config is OK
        if(!this.elasticBalancer.isLoaded) {
            throw new Exception("Elastic Balancer not loaded!");
        }

        // All OK, Run executors!
        Debugger.getInstance().printOutput(" Server started!");
        System.out.println("\n");
        this.runObserverRunnables();
    }

    /**
     * Observers the CortexServer is going to run periodically
     */
    private void runObserverRunnables() {

        // TODO: Move statusRunnable to custom/status
        Runnable statusRunnable = new Runnable() {
            public void run() {
                try {

                    Integer allContainers = elasticBalancer.getContainerManager().getAllContainers().size();
                    Integer stoppedContainers = elasticBalancer.getContainerManager().getStoppedContainers().size();
                    Integer runningContainers = elasticBalancer.getContainerManager().getRunningContainers().size();

                    Debugger.getInstance().printOutput("Containers - "+ allContainers +" Registered, "+ runningContainers +" Running, "+ stoppedContainers +" Stopped");

                    // TODO: log usage into STATSD if available

                } catch (Exception e) {
                    // @TODO: Display error message
                }
            }
        };
        statusThreadPool.scheduleAtFixedRate(statusRunnable, 5L, 5L, TimeUnit.SECONDS);

        Runnable containerStatusRunnable = new Runnable() {
            public void run() {
                elasticBalancer.getContainerManager().loadContainers();
            }
        };
        statusThreadPool.scheduleAtFixedRate(containerStatusRunnable, 5L, 5L, TimeUnit.SECONDS);


        /**
         * OBSERVERS AUTO-SETUP
         */
        // Iterate through Custom Observers file
        ArrayList<String> observerNames = ClassHelpers.getClassNamesFromPackage(this.configInstance.CUSTOM_OBSERVERS_PACKAGE_NAME);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        ArrayList<Runnable> loadedRunnables = new ArrayList<Runnable>();

        for (String observerClassName : observerNames) {
            try{
                AbstractObserver loadedObserver = ClassHelpers.instantiateClass(this.configInstance.CUSTOM_OBSERVERS_PACKAGE_NAME + "." + observerClassName, AbstractObserver.class);
                observerManager.add(loadedObserver);
            }catch (Exception e) { /* Fail silently */}

        }

        observerManager.startRunnables();


    }

}
