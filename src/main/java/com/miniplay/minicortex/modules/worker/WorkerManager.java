package com.miniplay.minicortex.modules.worker;

import com.miniplay.common.Debugger;
import com.miniplay.minicortex.config.Config;
import com.miniplay.minicortex.config.ConfigManager;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;
import com.miniplay.minicortex.modules.worker.drivers.AbstractWorkerDriver;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


public class WorkerManager {

    protected ElasticBalancer elasticBalancer = null;
    private Config config = null;
    public Boolean isLoaded = false;
    protected AbstractWorkerDriver workerDriver = null;

    /**
     * DockerMachineDriver constructor
     * @param elasticBalancer ElasticBalancer
     */
    public WorkerManager(ElasticBalancer elasticBalancer) {
        this.elasticBalancer = elasticBalancer;
        this.loadConfig();

        System.out.println(Debugger.PREPEND_OUTPUT + "DockerMachineDriver Loaded OK");
    }

    /**
     * Load conf into obj
     */
    private void loadConfig() {
        this.config = ConfigManager.getConfig();
        this.isLoaded = true;
    }

    public AbstractWorkerDriver getWorkerDriver() {

        try {
            Class workerDriverClass = Class.forName(this.config.WORKER_DRIVER);
            Constructor workerDriverConstructor = workerDriverClass.getConstructor(ElasticBalancer.class);
            Object workerDriver = workerDriverConstructor.newInstance(elasticBalancer);

        } catch(ClassNotFoundException e) {
            Debugger.getInstance().print("Instance Worker Driver ClassNotFoundException [" + this.config.WORKER_DRIVER + "] - message: " + e.getMessage(),this.getClass());
        } catch (InstantiationException e) {
            Debugger.getInstance().print("Instance Worker Driver InstantiationException [" + this.config.WORKER_DRIVER + "] - message: " + e.getMessage(),this.getClass());
        } catch (IllegalAccessException e) {
            Debugger.getInstance().print("Instance Worker Driver IllegalAccessException [" + this.config.WORKER_DRIVER + "] - message: " + e.getMessage(),this.getClass());
        } catch (InvocationTargetException e) {
            Debugger.getInstance().print("Instance Worker Driver InvocationTargetException [" + this.config.WORKER_DRIVER + "] - message: " + e.getMessage(),this.getClass());
        } catch (NoSuchMethodException e) {
            Debugger.getInstance().print("Instance Worker Driver NoSuchMethodException [" + this.config.WORKER_DRIVER + "] - message: " + e.getMessage(),this.getClass());
        }

        return workerDriver;

    }


}
