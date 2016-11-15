package com.miniplay.minicortex.modules.worker.drivers;

import com.miniplay.common.Debugger;
import com.miniplay.minicortex.config.Config;
import com.miniplay.minicortex.exceptions.InvalidProvisionParams;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;
import com.miniplay.minicortex.modules.worker.drivers.docker.Worker;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

abstract public class AbstractWorkerDriver {

    private Config config = null;
    protected ElasticBalancer elasticBalancer = null;

    public Boolean isLoaded = false;
    public Boolean isActive = false;

    public volatile ConcurrentHashMap<String, AbstractWorker> workers = new ConcurrentHashMap<String, AbstractWorker>();

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

    public AbstractWorker getWorker(String name){return null;}

    abstract public ArrayList<? extends AbstractWorker> getAllWorkers();
    abstract public ArrayList<? extends AbstractWorker> getRunningWorkers();
    abstract public ArrayList<? extends AbstractWorker> getStoppedWorkers();

    private AbstractWorker getRandomWorker(String state){return null;}

    /**
     * @return config Config
     */
    public Config getConfig(){return null;}

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
     * Setters
     */

    public void loadWorkers(){}

    private void loadConfig(){}

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

    public void loadContainers() {}

    public Boolean registerWorker(String name, String driver, String state, String url){return null;}

    public void provisionWorker(String workerName) throws InvalidProvisionParams{}

    public void provisionWorkers(Integer workersToProvision){}


    /**
     * actions over workers
     */

    public void killWorkers(Integer workersToKill){}

    public void startWorkers(Integer workersToStart){}

    private void killRandomWorker(){}

    private void startRandomWorker(){}

}