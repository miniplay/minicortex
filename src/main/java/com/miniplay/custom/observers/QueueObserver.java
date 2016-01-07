package com.miniplay.custom.observers;
import com.google.gson.Gson;
import com.miniplay.common.Stats;
import com.miniplay.custom.ObserverHelpers.Queue.StatusMessage;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;
import com.miniplay.minicortex.observers.AbstractObserver;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * Created by ret on 7/12/15.
 */
public class QueueObserver extends AbstractObserver {

    public static final String QUEUE_STATUS_URL = "http://api.minijuegos.com/external/monitoring/gearman/jobs?json=1";
    public Gson gson = new Gson();
    public static final String LOG_PREPEND = "> Queue Observer: ";

    public void runObserver() {
        System.out.println(LOG_PREPEND + "Updating queue values...");
        String queueStatusOutput = this.fetch();

        StatusMessage statusMessage = gson.fromJson(queueStatusOutput, StatusMessage.class);

        ElasticBalancer.getInstance().workers.set(statusMessage.workers);
        ElasticBalancer.getInstance().workers_queued_jobs.set(statusMessage.workers_queued_jobs);

        System.out.println(LOG_PREPEND + "\t"
                + ElasticBalancer.getInstance().workers + " [WORKERS] \t"
                + ElasticBalancer.getInstance().workers_queued_jobs + " [WORKER_QUEUED_JOBS] \t"
        );

        if(Stats.getInstance().isEnabled()) {
            Stats.getInstance().get().increment("minicortex.observers.queue.executions");
            Stats.getInstance().get().gauge("minicortex.observers.queue.workers", statusMessage.workers);
            Stats.getInstance().get().gauge("minicortex.observers.queue.workers_queued_jobs", statusMessage.workers_queued_jobs);
        }
    }

    public void setConfig () {
        this.secsSpanBeforeStart = 5L;
        this.secsIntervalSpan = 60L;
    }

    public String fetch() {

        try {
            URL url = new URL(QUEUE_STATUS_URL);
            String output = "";
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
            for (String line; (line = reader.readLine()) != null;) {
                output += line;
            }
            return output;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
