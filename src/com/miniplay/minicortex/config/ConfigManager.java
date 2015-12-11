package com.miniplay.minicortex.config;

/**
 * Created by vxc on 11/12/15.
 */
public class ConfigManager {

    private static Config configInstance = null;

    protected static String DEFAULT_ENV = "dev";

    /**
     *
     * @param environment EnvironmentManager
     * @return Config
     */
    public static Config getConfig(EnvironmentManager environment){
        if(configInstance == null) {
            configInstance = new Config(environment);
        }
        return configInstance;
    }

    /**
     * @return Config
     */
    public static Config getConfig(){
        if(configInstance == null) {
            try {
                EnvironmentManager environment = new EnvironmentManager(DEFAULT_ENV);
                configInstance = new Config(environment);
            }catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return configInstance;
    }

}
