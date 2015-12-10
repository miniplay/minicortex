package com.miniplay.minicortex.observers;

/**
 * Created by vxc on 9/12/15.
 */
public abstract class AbstractObserver {

    public Runnable runnableInstance = null;

    public long secsSpanBeforeStart = 0L;

    public long secsIntervalSpan = 0L;

    public abstract void runObserver();

    public abstract void setConfig();

    public AbstractObserver() {
        setConfig();
    }

    public Runnable getRunnable() {
        if (runnableInstance == null){
            runnableInstance = new Runnable() {
                public void run() {
                    runObserver();
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

}
