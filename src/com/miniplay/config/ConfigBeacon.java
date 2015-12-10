package com.miniplay.config;

import com.miniplay.minicortex.config.Config;

import java.io.File;
import java.net.URL;

/**
 * Pointer to the Config File
 * Created by vxc on 10/12/15.
 */
public class ConfigBeacon {

    public File getConfigFile() {
        URL url = getClass().getResource(Config.CUSTOM_CONFIG_FILE_NAME);
        return new File(url.getPath());
    }
}
