package com.miniplay.custom.observers;
import com.miniplay.common.CommandExecutor;
import com.miniplay.common.Stats;
import com.miniplay.minicortex.exceptions.DependenciesNotInstalled;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;
import com.miniplay.minicortex.modules.worker.drivers.docker.DockerDriver;
import com.miniplay.minicortex.modules.worker.drivers.dockermachine.DockerMachineDriver;
import com.miniplay.minicortex.observers.AbstractObserver;

import java.io.IOException;

public class DockerObserver extends AbstractObserver {

    public static final String LOG_PREPEND = "> Docker Container Observer: ";

    private void checkDependencies() throws DependenciesNotInstalled {
        try {
            CommandExecutor.getInstance().execute("docker help");
        }catch(IOException e) {
            throw new DependenciesNotInstalled("Package 'docker' is not installed, minicortex cannot run without it");
        }
    }

    public void runObserver() throws Exception {

        // Check dependencies first
        try {
            this.checkDependencies();
        } catch (DependenciesNotInstalled e) {
            System.out.println( LOG_PREPEND+ " Observer dependencies not installed, please ensure docker is installed");
        }

        System.out.println( LOG_PREPEND+ "Loading driver...");
        if (!(ElasticBalancer.getInstance().getWorkerManager().getWorkerDriver() instanceof DockerDriver)) {
            throw new Exception("WorkerManager.getWorkerDriver() returned an instance different than DockerDriver. Exiting.");
        }

        DockerDriver currentDriver = (DockerDriver) ElasticBalancer.getInstance().getWorkerManager().getWorkerDriver();

        System.out.println( LOG_PREPEND+ "Loading workers (docker containers)...");

        currentDriver.loadWorkers();

        Integer allContainers = currentDriver.getAllWorkers().size();
        Integer runningContainers = currentDriver.getRunningWorkers().size();
        Integer exitedContainers = currentDriver.getStoppedWorkers().size();
        Integer pausedContainers = currentDriver.getPausedWorkers().size();
        Integer createdContainers = currentDriver.getCreatedWorkers().size();

        System.out.println(LOG_PREPEND + "\t" +
                allContainers + " [REGISTER] \t" +
                runningContainers + " [RUNNING] \t" +
                exitedContainers + " [EXITED] \t" +
                pausedContainers + " [PAUSED] \t" +
                createdContainers + " [CREATED] \t"
        );

        if(Stats.getInstance().isEnabled()) {
            Stats.getInstance().get().increment("minicortex.observers.docker.executions");
            Stats.getInstance().get().gauge("minicortex.observers.docker.registered", allContainers);
            Stats.getInstance().get().gauge("minicortex.observers.docker.running", runningContainers);
            Stats.getInstance().get().gauge("minicortex.observers.docker.exited", exitedContainers);
            Stats.getInstance().get().gauge("minicortex.observers.docker.paused", pausedContainers);
            Stats.getInstance().get().gauge("minicortex.observers.docker.created", createdContainers);
        }

    }

    public void setConfig () {
        this.secsSpanBeforeStart = 10L;
        this.secsIntervalSpan = 60L;
    }
    
}
