package com.miniplay.custom.observers;
import com.miniplay.minicortex.observers.AbstractObserver;

/**
 *
 * Created by ret on 7/12/15.
 */
public class QueueObserver extends AbstractObserver {

    public void runObserver() {
        System.out.println("Queue observer running...");
    }

    public void setConfig () {
        this.secsSpanBeforeStart = 2L;
        this.secsIntervalSpan = 4L;
    }

}
