package com.miniplay.minicortex.modules.balancer;

import com.miniplay.common.Debugger;
import com.miniplay.common.Stats;
import com.miniplay.minicortex.config.Config;
import com.miniplay.minicortex.config.ConfigManager;
import com.miniplay.minicortex.modules.docker.ContainerManager;
import com.miniplay.minicortex.server.CortexServer;
import com.timgroup.statsd.NonBlockingStatsDClient;

import javax.swing.plaf.basic.BasicTreeUI;
import java.security.InvalidParameterException;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
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
    private Integer EB_TOLERANCE_THRESHOLD = 0;
    public Boolean isLoaded = false;

    private Integer maxContainers = 2;
    private Integer minContainers = 1;
    private Integer maxBootsInLoop = 1;
    private Integer maxShutdownsInLoop = 1;
    private Integer minsShutdownLocked = 3;
    private long lastBootTs = 0L;

    /* Workers status */
    public AtomicInteger workers = new AtomicInteger();
    public AtomicInteger workers_queued_jobs = new AtomicInteger();

    /* Elastic balancer config */
    public ScheduledExecutorService balancerThreadPool = Executors.newScheduledThreadPool(2);
    Runnable balancerRunnable = null;



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
    private ElasticBalancer() {
        Debugger.getInstance().printOutput("Loading Elastic Balancer...");

        // Load & validate config
        this.loadConfig();

        // Load Docker config
        this.containerManager = new ContainerManager(this);

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
        Config configInstance = ConfigManager.getConfig();
        System.out.println(configInstance.getElasticBalancerConfig());

        // Set if the ElasticBalancer is allowed to provision new containers
        this.EB_ALLOW_PROVISION_CONTAINERS = configInstance.EB_ALLOW_PROVISION_CONTAINERS;

        // Set the tolerance threshold for the ElasticBalancer
        if(configInstance.EB_TOLERANCE_THRESHOLD > 0) {
            this.EB_TOLERANCE_THRESHOLD = configInstance.EB_TOLERANCE_THRESHOLD;
        } else {
            throw new InvalidParameterException("EB_TOLERANCE_THRESHOLD parameter does not exist");
        }

        this.maxContainers = configInstance.DOCKER_MAX_CONTAINERS;
        this.minContainers = configInstance.DOCKER_MIN_CONTAINERS;
        this.maxBootsInLoop = configInstance.DOCKER_MAX_BOOTS_IN_LOOP;
        this.maxShutdownsInLoop = configInstance.DOCKER_MAX_SHUTDOWNS_IN_LOOP;
        this.minsShutdownLocked = configInstance.EB_SHUTDOWN_MINS_LOCK;

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
            Debugger.getInstance().debug("CALCULATED SCORE: " + balanceScore,this.getClass());
            Stats.getInstance().get().gauge("minicortex.elastic_balancer.balance.score",balanceScore);
            return balanceScore;
        } catch(Exception e) { // If we have any exception return the min number of containers set
            e.printStackTrace();
            return ConfigManager.getConfig().DOCKER_MIN_CONTAINERS;
        }
    }

    /**
     * Elastic balance containers based on the calculated balance score
     * @param balanceScore Integer
     */
    private void elasticBalanceContainers(Integer balanceScore) {
        Config configInstance = ConfigManager.getConfig();
        Integer runningWorkers = this.workers.get();
        Integer runningContainers = this.getContainerManager().getRunningContainers().size();
        Integer containerScore = configInstance.DOCKER_MIN_CONTAINERS; // Equaling to minimum

        if(runningContainers.intValue() != runningWorkers.intValue()) {
            Debugger.getInstance().print("Workers & Containers doesn't match [ "+runningWorkers+" Workers vs "+runningContainers+" Containers ]",this.getClass());
        }

        if (!this.checkMinimumBalanceRequirements()) {return;}

        containerScore = Math.abs(balanceScore);
        int scoreSign = Integer.signum(balanceScore);

        switch (scoreSign) {
            // Negative number. Remove containers case
            case -1:

                Integer containersToKill = Math.abs(containerScore);

                // CASE: Willing to remove more containers than the minimum set
                if((runningWorkers - containerScore) < this.minContainers) {
                    containersToKill = runningWorkers - this.minContainers;
                }

                if(containersToKill > this.maxShutdownsInLoop) { // Check DOCKER_MAX_SHUTDOWNS_IN_LOOP
                    Debugger.getInstance().print("## INFO: Containers to kill limit reached! Want to kill "+containersToKill+" and MAX is "+ this.maxShutdownsInLoop,this.getClass());
                    containersToKill = this.maxShutdownsInLoop;
                }

                // Check if we can remove machines

                if (containersToKill == 0) {
                    Debugger.getInstance().debug("--> NEGATIVE SCORE: " + runningWorkers + " active workers, No containers to remove (MIN_CONTAINERS = "+ this.minContainers +").",this.getClass());
                } else {
                    Debugger.getInstance().debug("--> NEGATIVE SCORE: " + runningWorkers + " active workers, " + containersToKill + " to remove.",this.getClass());
                }

                this.removeContainers(containersToKill);
                break;

            // Null case. Keep containers number
            case 0:
                Debugger.getInstance().debug("--> ZERO SCORE: " + runningWorkers + " active workers, nothing to do. Score " + containerScore,this.getClass());
                break;

            // Positive number. Provision containers case
            case 1:

                Integer containersToStart = Math.abs(containerScore);

                if((runningWorkers + containerScore) > this.maxContainers) {
                    containersToStart = this.maxContainers - runningWorkers;
                }

                if(containersToStart > this.maxBootsInLoop) { // Check DOCKER_MAX_BOOTS_IN_LOOP
                    Debugger.getInstance().print("## INFO: Containers to start limit reached! Want to boot "+containersToStart+" and MAX is "+ this.maxBootsInLoop,this.getClass());
                    containersToStart = this.maxBootsInLoop;
                }

                if (containersToStart == 0) {
                    Debugger.getInstance().debug("--> POSITIVE SCORE: " + runningWorkers + " active workers, No containers to boot up (MAX_CONTAINERS = "+ this.maxContainers +").",this.getClass());
                } else {
                    Debugger.getInstance().debug("--> POSITIVE SCORE: " + runningWorkers + " active workers, + " + containersToStart + " containers to start.",this.getClass());
                }

                this.addContainers(containersToStart);
                break;
        }

    }

    private boolean checkMinimumBalanceRequirements() {
        boolean result = true;
        Integer runningWorkers = this.workers.get();

        /**
         * CASE : Workers present does NOT meet the MIN value
         */
        if (runningWorkers < this.minContainers) {
            int containersNeeded = (this.minContainers - runningWorkers);
            int howManyToBoot = 1;
            if (containersNeeded >= this.maxBootsInLoop) {
                howManyToBoot = this.maxBootsInLoop;
            } else {
                howManyToBoot = containersNeeded;
            }
            Debugger.getInstance().debug("BALANCING REQUIREMENTS NOT MET: Workers present (" + runningWorkers + ") are below minimum value (" + this.minContainers + "), Booting up "+howManyToBoot+ " container",this.getClass());
            this.addContainers(howManyToBoot);
            result = false;
        /**
         * CASE : MORE CONTAINERS THAN MAX VALUE
         */
        } else if (runningWorkers > this.maxContainers) {
            int containersNotNeeded = (runningWorkers - this.maxContainers);
            int howManyToRemove = 1;
            if (containersNotNeeded >= this.maxShutdownsInLoop) {
                howManyToRemove = this.maxShutdownsInLoop;
            } else {
                howManyToRemove = containersNotNeeded;
            }
            Debugger.getInstance().debug("BALANCING REQUIREMENTS NOT MET: Workers present (" + runningWorkers + ") are above maximum value (" + this.maxContainers + "), Removing "+howManyToRemove+" container",this.getClass());
            this.removeContainers(howManyToRemove);
            result = false;
        }

        return result;
    }

    /**
     * Wrapper to Boot-up containers
     * @param howMany integer
     */
    private void addContainers(int howMany) {
        if (howMany == 0) {return;}
        Debugger.getInstance().debug("Booting " + howMany + " container..",this.getClass());
        this.getContainerManager().startContainers(howMany);
        this.refreshShutdownLockTs();
        Stats.getInstance().get().gauge("minicortex.elastic_balancer.balance.containers.started",howMany);
    }

    /**
     * Wrapper to Remove containers
     * @param howMany integer
     */
    private void removeContainers(int howMany) {
        if (howMany == 0) {return;}
        if (!this.canShutdownContainer()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd 'at' hh:mm:ss");
            String date = sdf.format(this.lastBootTs*1000);
            Debugger.getInstance().debug("#INFO: Containers can't be removed yet due to  " + this.minsShutdownLocked + " mins lock after last container boot @ "+date,this.getClass());
            return;
        }
        Debugger.getInstance().debug("Removing " + howMany + " container..",this.getClass());
        this.getContainerManager().killContainers(howMany);
        Stats.getInstance().get().gauge("minicortex.elastic_balancer.balance.containers.killed",howMany);
    }

    private boolean canShutdownContainer() {
        long nowTs = System.currentTimeMillis() / 1000L;
        return (nowTs >= this.lastBootTs + (this.minsShutdownLocked*60 ) );
    }

    private void refreshShutdownLockTs() {
        this.lastBootTs = System.currentTimeMillis() / 1000L;
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
            elasticBalanceContainers(balanceScore);
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
}
