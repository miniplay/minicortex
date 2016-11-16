package com.miniplay.minicortex.modules.worker.drivers;

import com.miniplay.common.Debugger;
import com.miniplay.minicortex.config.Config;
import com.miniplay.minicortex.exceptions.InvalidProvisionParams;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

abstract public class AbstractWorkerDriver {

    private Config config = null;
    protected ElasticBalancer elasticBalancer = null;

    public Boolean isLoaded = false;
    public Boolean isActive = false;

    public volatile ConcurrentHashMap<String, AbstractWorker> workers = new ConcurrentHashMap<String, AbstractWorker>();
    public volatile ConcurrentHashMap<String, AbstractWorker> workersScheduledStart = new ConcurrentHashMap<String, AbstractWorker>();
    public volatile ConcurrentHashMap<String, AbstractWorker> workersScheduledStop = new ConcurrentHashMap<String, AbstractWorker>();

    private Integer minWorkers;
    private Integer maxWorkers;
    private Integer maxShutdownsInLoop;
    private Integer maxBootsInLoop;


    /**
     * AbstractWorkerDriver constructor
     * @param elasticBalancer ElasticBalancer
     */
    public AbstractWorkerDriver(ElasticBalancer elasticBalancer) {
        this.elasticBalancer = elasticBalancer;
        this.loadConfig();

        System.out.println(Debugger.PREPEND_OUTPUT + "AbstractWorkerDriver Loaded OK");
    }


    /**
     * Getters
     */

    /**
     * @param name String
     * @return AbstractWorker
     */
    abstract public AbstractWorker getWorker(String name);

    /**
     * @return ArrayList
     */
    abstract public ArrayList<? extends AbstractWorker> getAllWorkers();

    /**
     * @return ArrayList
     */
    abstract public ArrayList<? extends AbstractWorker> getRunningWorkers();

    /**
     * @return ArrayList
     */
    abstract public ArrayList<? extends AbstractWorker> getStoppedWorkers();

    /**
     * @param state String
     * @return AbstractWorker
     */
    private AbstractWorker getRandomWorker(String state){return null;}

    /**
     * @return config Config
     */
    abstract public Config getConfig();

    /**
     * @return maxWorkers Integer
     */
    public Integer getMaxWorkers() {
        return maxWorkers;
    }

    /**
     * @return minWorkers Integer
     */
    public Integer getMinWorkers() {
        return minWorkers;
    }

    /**
     * @return maxBootsInLoop Integer
     */
    public Integer getMaxBootsInLoop() {
        return maxBootsInLoop;
    }

    /**
     * @return maxShutdownsInLoop Integer
     */
    public Integer getMaxShutdownsInLoop() {
        return maxShutdownsInLoop;
    }


    /**
     * Loaders
     */

    abstract public void loadWorkers();

    private void loadConfig(){}


    /**
     * Setters
     */


    /**
     * @param minWorkers Integer
     */
    public void setMinWorkers(Integer minWorkers) {
        this.minWorkers = minWorkers;
    }

    /**
     * @param maxWorkers Integer
     */
    public void setMaxWorkers(Integer maxWorkers) {
        this.maxWorkers = maxWorkers;
    }

    /**
     * @param maxShutdownsInLoop Integer
     */
    public void setMaxShutdownsInLoop(Integer maxShutdownsInLoop) {
        this.maxShutdownsInLoop = maxShutdownsInLoop;
    }

    /**
     * @param maxBootsInLoop Integer
     */
    public void setMaxBootsInLoop(Integer maxBootsInLoop) {
        this.maxBootsInLoop = maxBootsInLoop;
    }


    /**
     * Provision and register
     */

    /**
     * @param name String
     * @return Boolean
     */
    public Boolean registerWorker(String name) { return false; }

    /**
     * @param workerName String
     * @throws InvalidProvisionParams
     */
    abstract public void provisionWorker(String workerName) throws InvalidProvisionParams;

    /**
     * @param workersToProvision Integer
     */
    abstract public void provisionWorkers(Integer workersToProvision);


    /**
     * actions over workers
     */

    /**
     * @param workersToKill Integer
     */
    abstract public void killWorkers(Integer workersToKill);

    /**
     * @param workersToStart Integer
     */
    abstract public void startWorkers(Integer workersToStart);

    private void killRandomWorker(){}

    private void startRandomWorker(){}

}