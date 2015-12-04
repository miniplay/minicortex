package com.miniplay.minicortex.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by ret on 4/12/15.
 */
public class CortexServer {


    public ScheduledExecutorService executorThreadPoolServiceStatus = Executors.newScheduledThreadPool(2);

    /**
     * CortexServer constructor
     */
    public CortexServer() {

        // @TODO: Load Elastic Balancer config

        // @TODO: Initialize event loop

    }

    /**
     * Let's run the CortexServer
     */
    public void run() {
        // @TODO: Check balancer config is OK

        // Run executors!
        this.runObserverRunnables();
    }

    /**
     * Observers the CortexServer is going to run periodically
     */
    private void runObserverRunnables() {

        Runnable statusRunnable = new Runnable() {
            public void run() {
                try {

                    // @TODO: Get server usage
                    // @TODO: log usage into statsd if available
                    // @TODO: Print output if enabled in config
                    System.out.println("> CortexServer: 0 Containers | 0 Pending Jobs");
                } catch (Exception e) {
                    // @TODO: Display error message
                }
            }
        };
        executorThreadPoolServiceStatus.scheduleAtFixedRate(statusRunnable, 2L, 2L, TimeUnit.SECONDS);

        Runnable queueRunnable = new Runnable() {
            public void run() {
                // @TODO: implement observer to run...
                System.out.println("\t > Queue Observer --");
            }
        };
        executorThreadPoolServiceStatus.scheduleAtFixedRate(queueRunnable, 5L, 30L, TimeUnit.SECONDS);

        Runnable dockerMangerRunnable = new Runnable() {
            public void run() {
                // @TODO: implement observer to run...
                System.out.println("\t > Docker Manager Observer --");
            }
        };
        executorThreadPoolServiceStatus.scheduleAtFixedRate(dockerMangerRunnable, 1L, 5L, TimeUnit.MINUTES);


    }

}
