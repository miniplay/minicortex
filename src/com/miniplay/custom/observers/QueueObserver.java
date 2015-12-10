package com.miniplay.custom.observers;
import com.miniplay.minicortex.observers.AbstractObserver;

import com.miniplay.common.Utils;

/**
 *
 * Created by ret on 7/12/15.
 */
public class QueueObserver extends AbstractObserver {

    public void runObserver() {
        System.out.println(Utils.PREPEND_OUTPUT_OBSERVERS + "Queue observer running...");
    }

    public void setConfig () {
        this.secsSpanBeforeStart = 2L;
        this.secsIntervalSpan = 4L;
    }

}
