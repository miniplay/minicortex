package com.miniplay.custom.observers;
import com.miniplay.common.Stats;
import com.miniplay.minicortex.config.Config;
import com.miniplay.minicortex.config.ConfigManager;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;
import com.miniplay.minicortex.observers.AbstractObserver;

import java.util.Random;

/**
 *
 * Created by ret on 7/12/15.
 */
public class TestObserver extends AbstractObserver {

    public static final String LOG_PREPEND = "> Container Observer: ";

    public void runObserver() {

        Config config = ConfigManager.getConfig();

        if (!config.isTestQueueMode()) {return;}

        int workerQueuedJobs = ElasticBalancer.getInstance().workers_queued_jobs.get();
        int newQueueValue = randInt(1,30);

        System.out.println(LOG_PREPEND + "\t"
                + " NEW [WORKER_QUEUED_JOBS] \t" + ElasticBalancer.getInstance().workers_queued_jobs.get()
        );


    }

    public void setConfig () {
        this.secsSpanBeforeStart = 13L;
        this.secsIntervalSpan = 60L;
    }


    public static int randInt(int min, int max) {

        Random rand = new Random();

        int randomNum = rand.nextInt((max - min) + 1) + min;

        return randomNum;
    }

}
