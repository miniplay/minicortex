package com.miniplay.minicortex.observers;

import com.miniplay.minicortex.exceptions.DependenciesNotInstalled;

/**
 * Created by vxc on 9/12/15.
 */
public abstract class AbstractObserver {

    public Runnable runnableInstance = null;

    public long secsSpanBeforeStart = 0L;

    public long secsIntervalSpan = 0L;

    public abstract void runObserver() throws Exception;

    public abstract void setConfig();

    public AbstractObserver() {
        setConfig();
    }

    public Runnable getRunnable() throws Exception {
        if (runnableInstance == null){
            runnableInstance = new Runnable() {
                public void run() {
                    try {
                        runObserver();
                    } catch(Exception e) {
                        System.out.println("Exception while running observer: " + e.getMessage());
                    }
                }
            };
        }
        return runnableInstance;
    };

    protected ObserverManager observerManager;

    public ObserverManager getObserverManager() {
        return observerManager;
    }

    public void setObserverManager(ObserverManager observerManager) {
        this.observerManager = observerManager;
    }

    private void checkDependencies() throws DependenciesNotInstalled { }

}
