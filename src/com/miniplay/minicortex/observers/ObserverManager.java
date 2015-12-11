package com.miniplay.minicortex.observers;

import com.miniplay.common.Debugger;
import com.sun.org.apache.xml.internal.resolver.helpers.Debug;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by vxc on 9/12/15.
 * Composite Pattern
 */
public class ObserverManager {

    public List<AbstractObserver> loggedObservers = new ArrayList<AbstractObserver>();

    public ScheduledExecutorService observersThreadPool = null;

    /**
     * @param observer
     */
    public void add(AbstractObserver observer) {
        this.loggedObservers.add(observer);
        observer.setObserverManager(this);
    }

    public void startRunnables() {

        // Initialize threadpool depending on observers length
        observersThreadPool = Executors.newScheduledThreadPool(loggedObservers.size());

        for (AbstractObserver loggedObserver : this.loggedObservers) {
            try{
                Runnable runnable = loggedObserver.getRunnable();
                Debugger.getInstance().print("Logging runnable " + loggedObserver.getClass() + " with " + loggedObserver.secsSpanBeforeStart + " secs as SpanSecsBeforeStart and " + loggedObserver.secsIntervalSpan + " secs as interval span", this.getClass());
                observersThreadPool.scheduleAtFixedRate(runnable, loggedObserver.secsSpanBeforeStart, loggedObserver.secsIntervalSpan, TimeUnit.SECONDS);
            }catch (Exception e) {
                Debugger.getInstance().print("Error while trying to schedule observer: " + e.getMessage(), this.getClass());
            }

        }
    }
}
