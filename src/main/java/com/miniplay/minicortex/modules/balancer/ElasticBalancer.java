package com.miniplay.minicortex.modules.balancer;

import com.miniplay.common.Debugger;
import com.miniplay.common.Stats;
import com.miniplay.minicortex.config.Config;
import com.miniplay.minicortex.config.ConfigManager;
import com.miniplay.minicortex.modules.worker.WorkerManager;

import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cortex Elastic Balancer
 * Created by ret on 4/12/15.
 */
public class ElasticBalancer {

    /* Elastic Balancer Conf */
    private Boolean EB_ALLOW_PROVISION_WORKERS = false;
    private Integer EB_TOLERANCE_THRESHOLD = 0;
    public Boolean isLoaded = false;

    /* Elastic Balancer Limits */
    private Integer maxWorkers = 2;
    private Integer minWorkers = 1;
    private Integer maxBootsInLoop = 1;
    private Integer maxShutdownsInLoop = 1;
    private Integer minsShutdownLocked = 3;
    private long lastBootTs = 0L;

    /* Workers status */
    public AtomicInteger workers = new AtomicInteger();
    public AtomicInteger workers_queued_jobs = new AtomicInteger();

    /* Elastic balancer claonfig */
    public ScheduledExecutorService balancerThreadPool = Executors.newScheduledThreadPool(2);
    Runnable balancerRunnable = null;



    /* Modules */
    private WorkerManager workerManager = null;


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
        Debugger.getInstance().print("Loading Elastic Balancer",this.getClass());

        // Load WorkerManager config
        this.workerManager = new WorkerManager(this);
        Debugger.getInstance().print("Loading Elastic Balancer: WorkerManager OK",this.getClass());

        // Load & validate config
        this.loadConfig();
        Debugger.getInstance().print("Loading Elastic Balancer: Config OK",this.getClass());

        // Start Balancer runnable
        Debugger.getInstance().print("Loading Elastic Balancer: Starting Balancer Runnable",this.getClass());
        this.startBalancerRunnable();

        // All OK!
        this.isLoaded = true;

        Debugger.getInstance().printOutput("Elastic Balancer Loaded OK");

    }

    public void triggerProvisionWorkers() {
        if(this.EB_ALLOW_PROVISION_WORKERS) { // Provision workers only if enabled in config
            Debugger.getInstance().printOutput("Triggered Worker Provision...");
            // Force first manual workers load
            getWorkerManager().getWorkerDriver().loadWorkers();
            Integer currentWorkers = getWorkerManager().getWorkerDriver().getAllWorkers().size();
            Integer maxWorkers = getWorkerManager().getWorkerDriver().getMaxWorkers();

            Debugger.getInstance().printOutput("Current workers " + currentWorkers + ", Max workers " + maxWorkers);

            if(maxWorkers > currentWorkers) {
                Integer workersToProvision = maxWorkers - currentWorkers;
                if(workersToProvision > maxWorkers) {
                    Debugger.getInstance().printOutput("MAX provision workers reached!");
                    workersToProvision = maxWorkers;
                }
                Debugger.getInstance().printOutput("Loading "+workersToProvision + " new workers");
                getWorkerManager().getWorkerDriver().provisionWorkers(workersToProvision);
            }
        }
    }

    private void loadConfig() {
        Config configInstance = ConfigManager.getConfig();
        System.out.println(configInstance.getElasticBalancerConfig());

        // Set if the ElasticBalancer is allowed to provision new workers
        this.EB_ALLOW_PROVISION_WORKERS = configInstance.EB_ALLOW_PROVISION_WORKERS;

        // Set the tolerance threshold for the ElasticBalancer
        if(configInstance.EB_TOLERANCE_THRESHOLD > 0) {
            this.EB_TOLERANCE_THRESHOLD = configInstance.EB_TOLERANCE_THRESHOLD;
        } else {
            throw new InvalidParameterException("EB_TOLERANCE_THRESHOLD parameter does not exist");
        }

        this.maxWorkers = getWorkerManager().getWorkerDriver().getMaxWorkers(); // configInstance.DOCKER_MACHINE_MAX_CONTAINERS;
        this.minWorkers = getWorkerManager().getWorkerDriver().getMinWorkers(); //configInstance.DOCKER_MACHINE_MIN_CONTAINERS;
        this.maxBootsInLoop = getWorkerManager().getWorkerDriver().getMaxBootsInLoop(); //configInstance.DOCKER_MACHINE_MAX_BOOTS_IN_LOOP;
        this.maxShutdownsInLoop = getWorkerManager().getWorkerDriver().getMaxShutdownsInLoop(); //configInstance.DOCKER_MACHINE_MAX_SHUTDOWNS_IN_LOOP;
        this.minsShutdownLocked = configInstance.EB_SHUTDOWN_MINS_LOCK;

    }

    /**
     * Calculates the balancer score, this score will be the decision to scale up or down workers
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
        } catch(Exception e) { // If we have any exception return the min number of workers set
            e.printStackTrace();
            return minWorkers;
        }
    }

    /**
     * Elastic balance workers based on the calculated balance score
     * @param balanceScore Integer
     */
    private void elasticBalanceWorkers(Integer balanceScore) { // @ TODO: Have a look at this, some variables overlapped when refactoring!!
        Config configInstance = ConfigManager.getConfig();
        Integer runningWorkersFromQueue = this.workers.get();
        Integer runningWorkers = this.getWorkerManager().getWorkerDriver().getRunningWorkers().size();
        Integer workerScore = minWorkers; // Equaling to minimum

        if(runningWorkersFromQueue.intValue() != runningWorkers.intValue()) {
            Debugger.getInstance().print("Queue Workers & Workers doesn't match [ "+runningWorkersFromQueue+" Workers From Queue vs "+runningWorkers+" Workers ]",this.getClass());
        }

        if (!this.checkMinimumBalanceRequirements()) {return;}

        workerScore = Math.abs(balanceScore);
        int scoreSign = Integer.signum(balanceScore);

        switch (scoreSign) {
            // Negative number. Remove containers case
            case -1:

                // Get containers scheduled kill
                Integer workersScheduledKill = this.getWorkerManager().getWorkerDriver().workersScheduledStop.size();
                Integer WorkersToKill = Math.abs(workerScore) - workersScheduledKill;
                if(workersScheduledKill > 0) Debugger.getInstance().print("## INFO: Found "+workersScheduledKill+" containers scheduled kill, taking off from "+WorkersToKill+" containers to kill",this.getClass());

                // CASE: Willing to remove more containers than the minimum set
                if((runningWorkers - workerScore) < this.minWorkers) {
                    WorkersToKill = runningWorkers - this.minWorkers;
                }

                if(WorkersToKill > this.maxShutdownsInLoop) { // Check DOCKER_MACHINE_MAX_SHUTDOWNS_IN_LOOP
                    Debugger.getInstance().print("## INFO: Workers to kill limit reached! Want to kill "+WorkersToKill+" and MAX is "+ this.maxShutdownsInLoop,this.getClass());
                    WorkersToKill = this.maxShutdownsInLoop;
                }

                // Check if we can remove machines

                if (WorkersToKill == 0) {
                    Debugger.getInstance().debug("--> NEGATIVE SCORE: " + runningWorkers + " active workers, No workers to remove (MIN_WORKERS = "+ this.minWorkers +").",this.getClass());
                } else {
                    Debugger.getInstance().debug("--> NEGATIVE SCORE: " + runningWorkers + " active workers, " + WorkersToKill + " to remove.",this.getClass());
                }

                this.removeWorkers(WorkersToKill);
                break;

            // Null case. Keep containers number
            case 0:
                Debugger.getInstance().debug("--> ZERO SCORE: " + runningWorkers + " active workers, nothing to do. Score " + workerScore,this.getClass());
                break;

            // Positive number. Provision containers case
            case 1:

                // Get workers scheduled start
                Integer workersScheduledStart = this.getWorkerManager().getWorkerDriver().workersScheduledStart.size();
                Integer workersToStart = Math.abs(workerScore) - workersScheduledStart;
                if(workersScheduledStart > 0) Debugger.getInstance().print("## INFO: Found "+workersScheduledStart+" workers scheduled start, taking off from "+workersToStart+" workers to start",this.getClass());


                if((runningWorkers + workerScore) > this.maxWorkers) {
                    workersToStart = this.maxWorkers - runningWorkers;
                }

                if(workersToStart > this.maxBootsInLoop) { // Check DOCKER_MACHINE_MAX_BOOTS_IN_LOOP
                    Debugger.getInstance().print("## INFO: Workers to start limit reached! Want to boot "+workersToStart+" and MAX is "+ this.maxBootsInLoop,this.getClass());
                    workersToStart = this.maxBootsInLoop;
                }

                if (workersToStart == 0) {
                    Debugger.getInstance().debug("--> POSITIVE SCORE: " + runningWorkers + " active workers, No containers to boot up (MAX_CONTAINERS = "+ this.maxWorkers +").",this.getClass());
                } else {
                    Debugger.getInstance().debug("--> POSITIVE SCORE: " + runningWorkers + " active workers, + " + workersToStart + " containers to start.",this.getClass());
                }

                this.addWorkers(workersToStart);
                break;
        }

    }

    private boolean checkMinimumBalanceRequirements() {
        boolean result = true;
        Integer runningWorkers = this.workers.get();

        /**
         * CASE : Workers present does NOT meet the MIN value
         */
        if (runningWorkers < this.minWorkers) {
            int containersNeeded = (this.minWorkers - runningWorkers);
            int howManyToBoot = 1;
            if (containersNeeded >= this.maxBootsInLoop) {
                howManyToBoot = this.maxBootsInLoop;
            } else {
                howManyToBoot = containersNeeded;
            }
            Debugger.getInstance().debug("BALANCING REQUIREMENTS NOT MET: Workers present (" + runningWorkers + ") are below minimum value (" + this.minWorkers + "), Booting up "+howManyToBoot+ " container",this.getClass());
            this.addWorkers(howManyToBoot);
            result = false;
        /**
         * CASE : MORE CONTAINERS THAN MAX VALUE
         */
        } else if (runningWorkers > this.maxWorkers) {
            int containersNotNeeded = (runningWorkers - this.maxWorkers);
            int howManyToRemove = 1;
            if (containersNotNeeded >= this.maxShutdownsInLoop) {
                howManyToRemove = this.maxShutdownsInLoop;
            } else {
                howManyToRemove = containersNotNeeded;
            }
            Debugger.getInstance().debug("BALANCING REQUIREMENTS NOT MET: Workers present (" + runningWorkers + ") are above maximum value (" + this.maxWorkers + "), Removing "+howManyToRemove+" container",this.getClass());
            this.removeWorkers(howManyToRemove);
            result = false;
        }

        return result;
    }

    /**
     * Wrapper to Boot-up containers
     * @param howMany integer
     */
    private void addWorkers(int howMany) {
        if (howMany == 0) {return;}
        Debugger.getInstance().debug("Booting " + howMany + " container..",this.getClass());
        this.getWorkerManager().getWorkerDriver().startWorkers(howMany);
        this.refreshShutdownLockTs();
        Stats.getInstance().get().gauge("minicortex.elastic_balancer.balance.containers.started",howMany);
    }

    /**
     * Wrapper to Remove containers
     * @param howMany integer
     */
    private void removeWorkers(int howMany) {
        if (howMany == 0) {return;}
        if (!this.canShutdownWorker()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd 'at' hh:mm:ss");
            String date = sdf.format(this.lastBootTs*1000);
            Debugger.getInstance().debug("#INFO: Workers can't be removed yet due to  " + this.minsShutdownLocked + " mins lock after last container boot @ "+date,this.getClass());
            return;
        }
        Debugger.getInstance().debug("Removing " + howMany + " container..",this.getClass());
        this.getWorkerManager().getWorkerDriver().killWorkers(howMany);
        Stats.getInstance().get().gauge("minicortex.elastic_balancer.balance.containers.killed",howMany);
    }

    private boolean canShutdownWorker() {
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
        Integer registeredWorkers = this.getWorkerManager().getWorkerDriver().getAllWorkers().size();
        Integer minWorkers = getWorkerManager().getWorkerDriver().getMinWorkers();
        if(registeredWorkers < minWorkers) {
            Debugger.getInstance().printOutput("Registered containers ("+registeredWorkers+") don't reach the minimum ("+minWorkers+"), ElasticBalance PAUSED!");
        } else {
            Debugger.getInstance().print("Calculating balancer score...",this.getClass());
            Integer balanceScore = calculateBalancerScore();
            elasticBalanceWorkers(balanceScore);
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
     * @return WorkerManager
     */
    public WorkerManager getWorkerManager() {
        return workerManager;
    }
}
