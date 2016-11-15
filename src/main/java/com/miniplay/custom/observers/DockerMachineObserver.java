package com.miniplay.custom.observers;
import com.miniplay.common.CommandExecutor;
import com.miniplay.common.Stats;
import com.miniplay.minicortex.exceptions.DependenciesNotInstalled;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;
import com.miniplay.minicortex.modules.worker.drivers.dockermachine.DockerMachineDriver;
import com.miniplay.minicortex.observers.AbstractObserver;

import java.io.IOException;

/**
 *
 * Created by ret on 7/12/15.
 */
public class DockerMachineObserver extends AbstractObserver {

    public static final String LOG_PREPEND = "> Docker-Machine Observer: ";

    private void checkDependencies() throws DependenciesNotInstalled {
        try {
            CommandExecutor.getInstance().execute("docker-machine help");
        }catch(IOException e) {
            throw new DependenciesNotInstalled("Package 'docker-machine' is not installed, minicortex cannot run without it");
        }
    }

    public void runObserver() throws Exception{

        // Check dependencies first
        try {
            this.checkDependencies();
        } catch (DependenciesNotInstalled e) {
            System.out.println( LOG_PREPEND+ " Observer dependencies not installed, please ensure docker-machine is installed");
        }


        System.out.println( LOG_PREPEND+ "Loading driver...");
        if (!(ElasticBalancer.getInstance().getWorkerManager().getWorkerDriver() instanceof DockerMachineDriver)) {
            throw new Exception("WorkerManager.getWorkerDriver() returned an instance different than DockerMachineDriver. Exiting.");
        }

        DockerMachineDriver currentDriver = (DockerMachineDriver) ElasticBalancer.getInstance().getWorkerManager().getWorkerDriver();

        System.out.println( LOG_PREPEND+ "Loading containers...");
        currentDriver.loadContainers();

        Integer allContainers = currentDriver.getAllWorkers().size();
        Integer stoppedContainers = currentDriver.getStoppedWorkers().size();
        Integer runningContainers = currentDriver.getRunningWorkers().size();
        Integer stoppingContainers = currentDriver.getStoppingWorkers().size();
        Integer startingContainers = currentDriver.getStartingWorkers().size();
        Integer errorContainers = currentDriver.getErrorWorkers().size();

        System.out.println(LOG_PREPEND + "\t" +
                allContainers + " [REGISTER] \t" +
                runningContainers + " [RUNNING] \t" +
                stoppedContainers + " [STOPPED] \t" +
                stoppingContainers + " [STOPPING] \t" +
                startingContainers + " [STARTING] \t " +
                errorContainers + " [ERROR] \t "
        );

        if(Stats.getInstance().isEnabled()) {
            Stats.getInstance().get().increment("minicortex.observers.container.executions");
            Stats.getInstance().get().gauge("minicortex.observers.container.registered", allContainers);
            Stats.getInstance().get().gauge("minicortex.observers.container.running", runningContainers);
            Stats.getInstance().get().gauge("minicortex.observers.container.stopped", stoppedContainers);
            Stats.getInstance().get().gauge("minicortex.observers.container.stopping", stoppingContainers);
            Stats.getInstance().get().gauge("minicortex.observers.container.starting", startingContainers);
        }

    }

    public void setConfig () {
        this.secsSpanBeforeStart = 10L;
        this.secsIntervalSpan = 60L;
    }
    
}
