package com.miniplay.custom.observers;
import com.miniplay.common.Debugger;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;
import com.miniplay.minicortex.observers.AbstractObserver;

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

        System.out.println(LOG_PREPEND + "\t" +
                allContainers +" [REGISTER] \t"+
                runningContainers +" [RUNNING] \t"+
                stoppedContainers +" [STOPPED] \t" +
                stoppingContainers +" [STOPPING] \t" +
                startingContainers +" [STARTING] \t "
        );
    }

    public void setConfig () {
        this.secsSpanBeforeStart = 10L;
        this.secsIntervalSpan = 60L;
    }
    
}
