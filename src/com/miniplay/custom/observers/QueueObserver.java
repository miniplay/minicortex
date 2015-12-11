package com.miniplay.custom.observers;
import com.miniplay.common.Debugger;
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

    public static final String QUEUE_STATUS_URL = "http://api.minijuegos.com/external/monitoring/gearman/jobs";

    public void runObserver() {
        System.out.println(Debugger.PREPEND_OUTPUT_OBSERVERS + "Queue observer running...");
        String queueStatusOutput = this.fetch();
        System.out.println(queueStatusOutput);
    }

    public void setConfig () {
        this.secsSpanBeforeStart = 2L;
        this.secsIntervalSpan = 4L;
    }

    public String fetch() {

        try {
            URL url = new URL(QUEUE_STATUS_URL);
            try {
                String output = "";
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
                for (String line; (line = reader.readLine()) != null;) {
                    output += line;
                }
                return output;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

}
