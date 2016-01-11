package com.miniplay.custom.observers;
import com.miniplay.common.Debugger;
import com.miniplay.common.Stats;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;
import com.miniplay.minicortex.observers.AbstractObserver;
import com.miniplay.minicortex.server.CortexServer;

/**
 *
 * Created by ret on 7/12/15.
 */
public class ContainerObserver extends AbstractObserver {

    public static final String LOG_PREPEND = "> Container Observer: ";

    public void runObserver() {
        System.out.println( LOG_PREPEND+ "Loading containers...");

        ElasticBalancer.getInstance().getContainerManager().loadContainers();

        Integer allContainers = ElasticBalancer.getInstance().getContainerManager().getAllContainers().size();
        Integer stoppedContainers = ElasticBalancer.getInstance().getContainerManager().getStoppedContainers().size();
        Integer runningContainers = ElasticBalancer.getInstance().getContainerManager().getRunningContainers().size();
        Integer stoppingContainers = ElasticBalancer.getInstance().getContainerManager().getStoppingContainers().size();
        Integer startingContainers = ElasticBalancer.getInstance().getContainerManager().getStartingContainers().size();
        Integer errorContainers = ElasticBalancer.getInstance().getContainerManager().getErrorContainers().size();

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
