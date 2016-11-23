package com.miniplay.common;

import com.miniplay.minicortex.config.Config;
import com.miniplay.minicortex.config.ConfigManager;
import com.timgroup.statsd.NonBlockingStatsDClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * Created by ret on 9/12/15.
 */
public class Stats {

    private static Stats instance = null;

    private NonBlockingStatsDClient statsdClient;

    public Stats() {
        if(ConfigManager.getConfig().STATSD_HOST != null && ConfigManager.getConfig().STATSD_HOST != null) {
            statsdClient = new NonBlockingStatsDClient("minicortex_"+ConfigManager.getConfig().STATSD_PREFIX, ConfigManager.getConfig().STATSD_HOST, ConfigManager.getConfig().STATSD_PORT);
        } else {
            statsdClient = null;
        }
    }

    /**
     * Stats instance (Singleton)
     * @return Stats instance
     */
    public static Stats getInstance(){
        if(instance == null) {
            instance = new Stats();
        }
        return instance;
    }

    /**
     * Get StatsD client
     * @return NonBlockingStatsDClient
     */
    public NonBlockingStatsDClient get() {
        return statsdClient;
    }

    /**
     * Is statsd enabled?
     * @return Boolean
     */
    public Boolean isEnabled() {
        return this.get() != null;
    }

}
