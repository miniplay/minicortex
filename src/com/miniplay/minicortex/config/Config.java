package com.miniplay.minicortex.config;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import com.miniplay.config.ConfigBeacon;


public class Config {

    private EnvironmentManager environment;


    /***************
     * PRIVATE CONFIG
     ***************/

    /* MiniCortex */
    private String version = "0.0.1";
    public String CUSTOM_OBSERVERS_PACKAGE_NAME = "com.miniplay.custom.observers";
    public static String CUSTOM_CONFIG_FILE_NAME = "config.yml";

    /***************
     * CUSTOM CONFIG
     ***************/

    /* GENERAL */
    public boolean DEBUG = false;

    /* DOCKER */
    public String DOCKER_DEFAULT_DRIVER = "amazonec2";
    public Integer DOCKER_MIN_CONTAINERS = 1;
    public Integer DOCKER_MAX_CONTAINERS = 10;
    public Integer DOCKER_MAX_BOOTS_IN_LOOP = null;
    public Integer DOCKER_MAX_SHUTDOWNS_IN_LOOP = null;
    public Boolean DOCKER_TERMINATE_MODE = false;

    /* AMAZON EC2 DOCKER DRIVER */
    public String AMAZONEC2_REGION = "";
    public String AMAZONEC2_ACCESS_KEY = "";
    public String AMAZONEC2_SECRET_KEY = "";
    public String AMAZONEC2_VPC_ID = "";
    public String AMAZONEC2_ZONE = "";
    public String AMAZONEC2_SSH_USER = "";
    public String AMAZONEC2_INSTANCE_TYPE = "";
    public String AMAZONEC2_AMI = "";
    public String AMAZONEC2_SUBNET_ID = "";
    public String AMAZONEC2_SECURITY_GROUP = "";
    public Boolean AMAZONEC2_USE_PRIVATE_ADDRESS = true;
    public Boolean AMAZONEC2_PRIVATE_ADDRESS_ONLY = true;

    /* ELASTIC BALANCER */
    public Boolean EB_ALLOW_PRIVISION_CONTAINERS = true;
    public Integer EB_MAX_PROVISION_CONTAINERS = 5;

    /* STATSD */
    public String STATSD_HOST = "";
    public Integer STATSD_PORT = 8125;

    /* GEARMAN */
    public String GEARMAN_HOST = "";
    public Integer GEARMAN_PORT = 4730;


    /**************
     * CONSTRUCTORS
     **************/


    /**
     * Constructor
     * @param environment
     */
    protected Config(EnvironmentManager environment) {
        this.environment = environment;
        initConfig();
    }

    /**
     * Constructor
     */
    protected Config() {
        try {
            this.environment = new EnvironmentManager("dev");
        }catch (InterruptedException e) {
            e.printStackTrace();
        }
        initConfig();
    }

    public void initConfig() {

        Yaml yaml = new Yaml();
        ConfigBeacon configBeacon = new ConfigBeacon();
        File configFile = configBeacon.getConfigFile();

        try {

            InputStream fileInputStream = new FileInputStream(configFile);

            // Parse the YAML file and return the output as a series of Maps and Lists
            Map< String, Object> yamlConfigMap = (Map< String, Object>) yaml.load(fileInputStream);
            Field[] configClassFields = getClass().getFields();

            for (Field classField : configClassFields) {
                for (String yamlConfigKeyName : yamlConfigMap.keySet()) {
                    if (classField.getName().equals(yamlConfigKeyName)) {
                        if (yamlConfigMap.get(yamlConfigKeyName) == null){
                            classField.set(this, null);
                        } else {
                            classField.set(this, yamlConfigMap.get(yamlConfigKeyName));
                        }
                        yamlConfigMap.remove(yamlConfigKeyName);
                        break;
                    }

                }
            }

            if (! validateConfig() ) {
                throw new Exception("Config integrity check didn't pass, Halting");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (this.isDebug()) {
            System.out.println("[" + this.getClass() + "]: New config initialized (Environment: " + environment.getEnvironmentName() + ")");
        }

        System.out.println(this.AMAZONEC2_SECRET_KEY);
        System.out.println(this.AMAZONEC2_ACCESS_KEY);

    }

    /**************
     * HELPERS
     **************/

    protected boolean validateConfig() {
        return true; // @todo
    }

    /*********
     * GETTERS
     *********/

    /**
     * Get config from class attributes using reflection
     * @param type String
     * @return Map
     */
    public Map<String, Object> getConfig(String type) {
        Map<String, Object> requestedConfig = new HashMap<String, Object>();
        Field[] fields = Config.class.getFields();
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

    public boolean isDebug() {
        return this.DEBUG;
    }

    public EnvironmentManager getEnvironment() {return environment;}

    /*********
     * SETTERS
     *********/

}
