package com.miniplay.minicortex.modules.worker;

import com.miniplay.common.Debugger;
import com.miniplay.minicortex.config.Config;
import com.miniplay.minicortex.config.ConfigManager;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;
import com.miniplay.minicortex.modules.worker.drivers.AbstractWorkerDriver;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


public class WorkerManager {

    public final String DRIVER_CLASSNAME_APPENDIX = "drivers.";

    protected ElasticBalancer elasticBalancer = null;
    private Config config = null;
    public Boolean isLoaded = false;
    protected AbstractWorkerDriver workerDriver = null;

    /**
     * WorkerManager constructor
     * @param elasticBalancer ElasticBalancer
     */
    public WorkerManager(ElasticBalancer elasticBalancer) {
        this.elasticBalancer = elasticBalancer;
        this.loadConfig();
        this.loadDriver();

        System.out.println(Debugger.PREPEND_OUTPUT +  " " + this.config.WORKER_DRIVER + " Loaded OK");
    }

    /**
     * Load conf into obj
     */
    private void loadConfig() {
        this.config = ConfigManager.getConfig();
        this.isLoaded = true;
    }

    private void loadDriver() {
        String driverFullPackagePath = this.getClass().getPackage().getName() + "." + this.DRIVER_CLASSNAME_APPENDIX + this.config.WORKER_DRIVER;
        try {
            Class workerDriverClass = Class.forName(driverFullPackagePath);
            Constructor workerDriverConstructor = workerDriverClass.getConstructor(ElasticBalancer.class);
            Object workerDriver = workerDriverConstructor.newInstance(elasticBalancer);
            this.workerDriver = (AbstractWorkerDriver)workerDriver;

        } catch(ClassNotFoundException e) {
            Debugger.getInstance().print("Instance Worker Driver ClassNotFoundException [" + driverFullPackagePath + "] - message: " + e.getMessage(),this.getClass());
        } catch (InstantiationException e) {
            Debugger.getInstance().print("Instance Worker Driver InstantiationException [" + driverFullPackagePath + "] - message: " + e.getMessage(),this.getClass());
        } catch (IllegalAccessException e) {
            Debugger.getInstance().print("Instance Worker Driver IllegalAccessException [" + driverFullPackagePath + "] - message: " + e.getMessage(),this.getClass());
        } catch (InvocationTargetException e) {
            Debugger.getInstance().print("Instance Worker Driver InvocationTargetException [" + driverFullPackagePath + "] - message: " + e.getMessage(),this.getClass());
        } catch (NoSuchMethodException e) {
            Debugger.getInstance().print("Instance Worker Driver NoSuchMethodException [" + driverFullPackagePath + "] - message: " + e.getMessage(),this.getClass());
        }
    }

    public AbstractWorkerDriver getWorkerDriver() {
        if(workerDriver == null) {
            loadDriver();
        }
        return workerDriver;

    }


}
