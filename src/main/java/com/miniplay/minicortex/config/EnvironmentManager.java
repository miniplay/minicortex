package com.miniplay.minicortex.config;

import java.io.File;
import java.io.InterruptedIOException;

public class EnvironmentManager {
        
    protected String[] rawArgs;

    protected String configPath = null;

    protected static final String DEFAULT_CFG_PATH = "config_example.yml";

    /**
     * @param configPath String
     * @throws InterruptedException
     */
    public EnvironmentManager(String configPath) throws InterruptedException {
        if(this.validateConfigPath(configPath)) {
            this.configPath = configPath;
        } else {
            String workingDir = System.getProperty("user.dir");
            configPath = workingDir + "/" + configPath;
            if(this.validateConfigPath(configPath)) {
                this.configPath = configPath;
            } else {
                throw new InterruptedException("Config file " + configPath + " not valid!!");
            }
        }
    }

    public EnvironmentManager() throws InterruptedException {
        String workingDir = System.getProperty("user.dir");
        if(this.validateConfigPath(workingDir + "/" + DEFAULT_CFG_PATH)) {
            this.configPath = workingDir + "/" + DEFAULT_CFG_PATH;
        } else {
            throw new InterruptedException("Config file " + workingDir + "/" + DEFAULT_CFG_PATH + " not valid!!");
        }
    }

    /**
     * @return String
     */
    public String getConfigPath() {
        return this.configPath;
    }

    /**
     * @param configPath String
     * @return Boolean
     */
    public Boolean validateConfigPath(String configPath) {
        File f = new File(configPath);
        return f.exists() && !f.isDirectory();
    }

}
