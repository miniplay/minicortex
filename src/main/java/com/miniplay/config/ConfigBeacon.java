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
        URL url;
        String fileName = new String();
        if(environment.isDev()) {
            fileName = Config.CUSTOM_CONFIG_FILE_NAME_DEV;
            url = getClass().getResource(Config.CUSTOM_CONFIG_FILE_NAME_DEV);
        } else {
            fileName = Config.CUSTOM_CONFIG_FILE_NAME;
            url = getClass().getResource(Config.CUSTOM_CONFIG_FILE_NAME);
        }

        //return new File(url.getPath());
        return new File("/etc/minicortex/" + fileName);
    }
}
