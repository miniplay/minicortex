package com.miniplay.minicortex.server;

import com.miniplay.common.Utils;
import com.miniplay.custom.observers.ContainerObserver;
import com.miniplay.custom.observers.QueueObserver;
import com.miniplay.minicortex.config.Config;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by ret on 4/12/15.
 */
public class CortexServer {

    // Constants
    public static final Boolean DEBUG = true;

    // Cortex config
    protected Boolean showConsoleOutput = true;
    protected Boolean showServerExceptions = true;

    // Executors
    public ScheduledExecutorService statusThreadPool = Executors.newScheduledThreadPool(2);
    public ScheduledExecutorService observersThreadPool = Executors.newScheduledThreadPool(2);

    // Modules
    private ElasticBalancer elasticBalancer = null;

    // Observers
    private ContainerObserver containerObserver = null;
    private QueueObserver queueObserver = null;


    /**
     * CortexServer constructor
     */
    public CortexServer(Config config) {

        // Load app config
        this.showConsoleOutput = config.isShowServerConsoleOutput();
        this.showServerExceptions = config.isShowExceptions();

        // Instantiate modules
        this.elasticBalancer = new ElasticBalancer(this, config);

        // Register observers
        this.containerObserver = new ContainerObserver();
        this.queueObserver = new QueueObserver();

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
        Utils.getInstance().printOutput(" Server started!");
        System.out.println("\n");
        this.runObserverRunnables();
    }

    /**
     * Observers the CortexServer is going to run periodically
     */
    private void runObserverRunnables() {

        Runnable statusRunnable = new Runnable() {
            public void run() {
                try {

                    Integer allContainers = elasticBalancer.getContainerManager().getAllContainers().size();
                    Integer stoppedContainers = elasticBalancer.getContainerManager().getStoppedContainers().size();
                    Integer runningContainers = elasticBalancer.getContainerManager().getRunningContainers().size();

                    if(showConsoleOutput) {
                        Utils.getInstance().printOutput("Containers - "+ allContainers +" Registered, "+ runningContainers +" Running, "+ stoppedContainers +" Stopped");
                    }

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

        Runnable queueRunnable = new Runnable() {
            public void run() {
                queueObserver.run();
            }
        };
        observersThreadPool.scheduleAtFixedRate(queueRunnable, 5L, 5L, TimeUnit.SECONDS);

        Runnable containerRunnable = new Runnable() {
            public void run() {
                containerObserver.run();
            }
        };
        observersThreadPool.scheduleAtFixedRate(containerRunnable, 1L, 15L, TimeUnit.SECONDS);


    }

}
