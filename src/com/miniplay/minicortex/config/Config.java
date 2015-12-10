package com.miniplay.minicortex.config;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import com.miniplay.config.ConfigBeacon;


public class Config {

    private EnvironmentManager environment;


    /* MiniCortex */
    private boolean showServerConsoleOutput = true;
    private boolean showExceptions = true;
    private String version = "0.0.1";
    public String CUSTOM_OBSERVERS_PACKAGE_NAME = "com.miniplay.custom.observers";
    public static String CUSTOM_CONFIG_FILE_NAME = "config.yml";

    /**
     * CUSTOM CONFIG
     */

    /* DOCKER */
    public static String DOCKER_DEFAULT_DRIVER = "amazonec2";
    public static Integer DOCKER_MIN_CONTAINERS = 1;
    public static Integer DOCKER_MAX_CONTAINERS = 10;
    public static Integer DOCKER_MAX_BOOTS_IN_LOOP = null;
    public static Integer DOCKER_MAX_SHUTDOWNS_IN_LOOP = null;
    public static Boolean DOCKER_TERMINATE_MODE = false;

    /* AMAZON EC2 DOCKER DRIVER */
    public static String AMAZONEC2_REGION = "";
    public static String AMAZONEC2_ACCESS_KEY = "";
    public static String AMAZONEC2_SECRET_KEY = "";
    public static String AMAZONEC2_VPC_ID = "";
    public static String AMAZONEC2_ZONE = "";
    public static String AMAZONEC2_SSH_USER = "";
    public static String AMAZONEC2_INSTANCE_TYPE = "";
    public static String AMAZONEC2_AMI = "";
    public static String AMAZONEC2_SUBNET_ID = "";
    public static String AMAZONEC2_SECURITY_GROUP = "";
    public static Boolean AMAZONEC2_USE_PRIVATE_ADDRESS = true;
    public static Boolean AMAZONEC2_PRIVATE_ADDRESS_ONYL = true;

    /* ELASTIC BALANCER */
    public static Boolean EB_ALLOW_PRIVISION_CONTAINERS = true;
    public static Integer EB_MAX_PROVISION_CONTAINERS = 5;

    /* STATSD */
    public static String STATSD_HOST = "graphite.lab.minijuegos.com";
    public static Integer STATSD_PORT = 8125;

    /* GEARMAN */
    public String GEARMAN_HOST = "54.229.30.207";
    public Integer GEARMAN_PORT = 4730;

    public Config(EnvironmentManager environment) {
        this.environment = environment;
        initConfig();
    }

    public Config() {
        try {
            this.environment = new EnvironmentManager("dev");
        }catch (InterruptedException e) {
            e.printStackTrace();
        }
        initConfig();
    }

    public void initConfig() {
        // The path of your YAML file.
        ArrayList<String> key = new ArrayList<String>();
        ArrayList<String> value = new ArrayList<String>();
        Yaml yaml = new Yaml();
        ConfigBeacon configBeacon = new ConfigBeacon();
        File configFile = configBeacon.getConfigFile();

        try {
            InputStream ios = new FileInputStream(configFile);
            // Parse the YAML file and return the output as a series of Maps and Lists
            Map< String, Object> result = (Map< String, Object>) yaml.load(ios);
            for (Object name : result.keySet()) {
                key.add(name.toString());
                if (result.get(name) == null){
                    value.add(null);
                } else {
                    value.add(result.get(name).toString());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(key);
        System.out.println(value);
    }

    public void startServerConsoleOutput() {
        this.showServerConsoleOutput = true;
    }
    public void stopServerConsoleOutput() {
        this.showServerConsoleOutput = false;
    }

    /**
     * Get config from class attributes using reflection
     * @param type String
     * @return Map
     */
    public Map<String, Object> getConfig(String type) {
        Map<String, Object> requestedConfig = new HashMap<String, Object>();
        Field[] fields = Config.class.getDeclaredFields();
        for (Field field : fields) {
            String prop_name = field.getName();

            if(prop_name.contains(type.toUpperCase())) {
                try {
                    requestedConfig.put(prop_name,field.get(this));
                } catch (IllegalAccessException e) {
                    System.out.println("Error Loading Config PROP "+prop_name+" ");
                }
            }
        }
        return requestedConfig;
    }

    /**
     * Get Docker Config
     * @return Map
     */
    public Map<String, Object> getDockerConfig() {
        return this.getConfig("DOCKER");
    }

    /**
     * Get Amazon Config
     * @return Map
     */
    public Map<String, Object> getAmazonEC2Config() {
        return this.getConfig("AMAZONEC2");
    }

    /**
     * Get Elastic Balancer Config
     * @return Map
     */
    public Map<String, Object> getElasticBalancerConfig() {
        return this.getConfig("EB_");
    }

    /**
     * Getters
     */
    public EnvironmentManager getEnvironment() {return environment;}
    public boolean isShowExceptions() {return showExceptions;}
    public boolean isShowServerConsoleOutput() {return showServerConsoleOutput;}

    /**
     * Setters
     */
    public void enableShowExceptions() {showExceptions = true;}
    public void disableShowExceptions() {showExceptions = false;}
    public void setShowServerConsoleOutput(boolean showServerConsoleOutput) {this.showServerConsoleOutput = showServerConsoleOutput;}

}
