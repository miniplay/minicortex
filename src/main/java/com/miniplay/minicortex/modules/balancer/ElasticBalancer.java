package com.miniplay.minicortex.modules.balancer;

import com.miniplay.common.Debugger;
import com.miniplay.minicortex.config.Config;
import com.miniplay.minicortex.config.ConfigManager;
import com.miniplay.minicortex.modules.docker.ContainerManager;
import com.miniplay.minicortex.server.CortexServer;
import com.timgroup.statsd.NonBlockingStatsDClient;

import javax.swing.plaf.basic.BasicTreeUI;
import java.security.InvalidParameterException;
import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cortex Elastic Balancer
 * Created by ret on 4/12/15.
 */
public class ElasticBalancer {

    /* Elastic Balancer Conf */
    private Boolean EB_ALLOW_PROVISION_CONTAINERS = false;
    private Integer EB_MAX_PROVISION_CONTAINERS = 0;
    private Integer EB_TOLERANCE_THRESHOLD = 0;
    public Boolean isLoaded = false;

    /* Workers status */
    public AtomicInteger workers = new AtomicInteger();
    public AtomicInteger workers_queued_jobs = new AtomicInteger();

    /* Elastic balancer config */
    public ScheduledExecutorService balancerThreadPool = Executors.newScheduledThreadPool(2);
    Runnable balancerRunnable = null;

    /* Modules */
    private ContainerManager containerManager = null;

    private NonBlockingStatsDClient statsdClient;


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
    private ElasticBalancer() {
        Debugger.getInstance().printOutput("Loading Elastic Balancer...");

        // Load & validate config
        this.loadConfig();

        // Load Docker config
        this.containerManager = new ContainerManager(this);

        if(ConfigManager.getConfig().STATSD_HOST != null && ConfigManager.getConfig().STATSD_HOST != null) {
            statsdClient = new NonBlockingStatsDClient("", ConfigManager.getConfig().STATSD_HOST, ConfigManager.getConfig().STATSD_PORT);
        } else {
            statsdClient = null;
        }

        // Start Balancer runnable
        this.startBalancerRunnable();

        // All OK!
        this.isLoaded = true;

        Debugger.getInstance().printOutput("Elastic Balancer Loaded OK");

    }

    public void triggerProvisionContainers() {
        if(this.EB_ALLOW_PROVISION_CONTAINERS) { // Provision containers only if enabled in config
            Debugger.getInstance().printOutput("Triggered Container Provision...");
            // Force first manual containers load
            getContainerManager().loadContainers();
            Integer currentContainers = getContainerManager().getAllContainers().size();
            Integer maxContainers = ConfigManager.getConfig().DOCKER_MAX_CONTAINERS;

            Debugger.getInstance().printOutput("Current containers " + currentContainers + ", Max containers " + maxContainers);

            if(maxContainers > currentContainers) {
                Integer containersToProvision = maxContainers - currentContainers;
                if(containersToProvision > ConfigManager.getConfig().DOCKER_MAX_CONTAINERS) {
                    Debugger.getInstance().printOutput("MAX provision containers reached!");
                    containersToProvision = ConfigManager.getConfig().DOCKER_MAX_CONTAINERS;
                }
                Debugger.getInstance().printOutput("Loading "+containersToProvision + " new containers");
                getContainerManager().provisionContainers(containersToProvision);
            }
        }
    }

    private void loadConfig() {
        Config config = ConfigManager.getConfig();
        System.out.println(config.getElasticBalancerConfig());

        // Set if the ElasticBalancer is allowed to provision new containers
        this.EB_ALLOW_PROVISION_CONTAINERS = config.EB_ALLOW_PROVISION_CONTAINERS;

        // Set the tolerance threshold for the ElasticBalancer
        if(config.EB_TOLERANCE_THRESHOLD > 0) {
            this.EB_TOLERANCE_THRESHOLD = config.EB_TOLERANCE_THRESHOLD;
        } else {
            throw new InvalidParameterException("EB_TOLERANCE_THRESHOLD parameter does not exist");
        }
    }

    /**
     * Calculates the balancer score, this score will be the decision to scale up or down containers
     * @return Integer
     */
    private Integer calculateBalancerScore() {
        try {
            Integer workersQueuedJobs = this.workers_queued_jobs.get();
            Integer runningWorkers = this.workers.get();

            Integer balanceScore = Math.round((workersQueuedJobs - ( runningWorkers * this.EB_TOLERANCE_THRESHOLD)) / (this.EB_TOLERANCE_THRESHOLD));
            Debugger.getInstance().debug("Calculated score without config values: " + balanceScore,this.getClass());


            Integer maxContainers = ConfigManager.getConfig().DOCKER_MAX_CONTAINERS;
            Integer minContainers = ConfigManager.getConfig().DOCKER_MIN_CONTAINERS;
            Integer maxBootsInLoop = ConfigManager.getConfig().DOCKER_MAX_BOOTS_IN_LOOP;
            Integer maxShutdownsInLoop = ConfigManager.getConfig().DOCKER_MAX_SHUTDOWNS_IN_LOOP;
            Integer runningContainers = this.getContainerManager().getRunningContainers().size();
            Integer containersAfterBalance = 0;
            Boolean isRemoveContainers;

            if(runningContainers.intValue() != runningWorkers.intValue()) {
                Debugger.getInstance().print("Workers & Containers doesn't match [ "+runningWorkers+" Workers vs "+runningContainers+" Containers ]",this.getClass());
            }

            if(balanceScore < 0) {
                isRemoveContainers = true;
                Debugger.getInstance().debug("Negative score (removing containers) | " + runningWorkers + " workers " + balanceScore + " score = " + (runningWorkers - Math.abs(balanceScore)),this.getClass());
                if((runningWorkers - Math.abs(balanceScore)) < minContainers) containersAfterBalance = minContainers;
            } else {
                isRemoveContainers = false;
                Debugger.getInstance().debug("Positive score (adding containers) | " + runningWorkers + " workers + " + balanceScore + " score = " + (runningWorkers + balanceScore),this.getClass());
                if((runningWorkers + Math.abs(balanceScore)) > maxContainers) containersAfterBalance = maxContainers;
            }

            if(isRemoveContainers) {
                Integer containersToKill = runningContainers - containersAfterBalance;
                if(containersToKill > maxShutdownsInLoop) { // Check DOCKER_MAX_SHUTDOWNS_IN_LOOP
                    Debugger.getInstance().print("Max containers to kill limit reached! Want to kill "+containersToKill+" and MAX is "+ maxShutdownsInLoop,this.getClass());
                    containersToKill = maxShutdownsInLoop;
                }
                Debugger.getInstance().print("Killing " + containersToKill + " containers, left " + containersAfterBalance + " containers",this.getClass());
                this.getContainerManager().killContainers(containersToKill);
            } else {
                Integer containersToStart = containersAfterBalance - runningContainers;
                if(containersToStart > maxBootsInLoop) { // Check DOCKER_MAX_BOOTS_IN_LOOP
                    Debugger.getInstance().print("Max containers to start limit reached! Want to kill "+containersToStart+" and MAX is "+ maxBootsInLoop,this.getClass());
                    containersToStart = maxBootsInLoop;
                }
                Debugger.getInstance().print("Adding " + containersToStart + " containers, left " + containersAfterBalance + " containers",this.getClass());
                this.getContainerManager().startContainers(containersToStart);
            }

            return Math.abs(containersAfterBalance);

        } catch(Exception e) {
            e.printStackTrace();

            // If we have any exception return the min number of containers set
            return ConfigManager.getConfig().DOCKER_MIN_CONTAINERS;
        }


    }

    /**
     * Recalculate containers needed for CortexServer at this moment
     */
    private void balance() {
        Integer registeredContainers = this.getContainerManager().getAllContainers().size();
        Integer minContainers = ConfigManager.getConfig().DOCKER_MIN_CONTAINERS;
        if(registeredContainers < minContainers) {
            Debugger.getInstance().printOutput("Registered containers ("+registeredContainers+") don't reach the minimum ("+minContainers+"), ElasticBalance PAUSED!");
        } else {
            Debugger.getInstance().print("Calculating balancer score...",this.getClass());
            Integer balanceScore = calculateBalancerScore();
        }
    }

    /**
     * Start's the Balancer score calculator & container scale up/down runnable
     */
    private void startBalancerRunnable() {
        balancerRunnable = new Runnable() {
            public void run() {
                balance();
            }
        };
        Long balancerRunnableTimeBeforeStart = 15L;
        Long balancerRunnableTimeInterval = 60L;
        balancerThreadPool.scheduleAtFixedRate(balancerRunnable, balancerRunnableTimeBeforeStart, balancerRunnableTimeInterval, TimeUnit.SECONDS);
    }

    /**
     * @return ContainerManager
     */
    public ContainerManager getContainerManager() {
        return containerManager;
    }

    public NonBlockingStatsDClient getStatsd() {
        return statsdClient;
    }
}
