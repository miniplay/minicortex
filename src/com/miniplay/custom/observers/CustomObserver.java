package com.miniplay.custom.observers;
import com.miniplay.minicortex.observers.AbstractObserver;

/**
 *
 * Created by ret on 7/12/15.
 */
public class CustomObserver extends AbstractObserver {

    public void runObserver() {
        System.out.println("Custom observer running...");
    }

    public void setConfig () {
        this.secsSpanBeforeStart = 5L;
        this.secsIntervalSpan = 5L;
    }

}
