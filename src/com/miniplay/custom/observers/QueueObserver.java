package com.miniplay.custom.observers;

import com.miniplay.common.Utils;

/**
 *
 * Created by ret on 7/12/15.
 */
public class QueueObserver {

    public QueueObserver() {

    }

    public void run() {
        System.out.println(Utils.PREPEND_OUTPUT_OBSERVERS + "Queue observer running...");
    }
}
