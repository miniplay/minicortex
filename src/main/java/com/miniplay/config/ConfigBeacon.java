package com.miniplay.config;

import com.miniplay.minicortex.config.Config;
import com.miniplay.minicortex.config.EnvironmentManager;

import java.io.File;
import java.net.URL;

/**
 * Pointer to the Config File
 * Created by vxc on 10/12/15.
 */
public class ConfigBeacon {

    public File getConfigFile(EnvironmentManager environment) {
        return new File(environment.getConfigPath());
    }
}
