package com.miniplay.minicortex.config;


import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class Config {

    private EnvironmentManager environment;


    /* MiniCortex */
    private boolean showServerConsoleOutput = true;
    private boolean showExceptions = true;
    private String version = "0.0.1";


    /* DOCKER */
    public static String DOCKER_DEFAULT_DRIVER = "amazonec2";
    public static Integer DOCKER_MIN_CONTAINERS = 1;
    public static Integer DOCKER_MAX_CONTAINERS = 10;
    public static Integer DOCKER_MAX_BOOTS_IN_LOOP = null;
    public static Integer DOCKER_MAX_SHUTDOWNS_IN_LOOP = null;
    public static Boolean DOCKER_TERMINATE_MODE = false;

    /* AMAZON EC2 DOCKER DRIVER */
    public static String AMAZONEC2_REGION = "eu-west-1";
    public static String AMAZONEC2_ACCESS_KEY = "AKIAIXSAJTUDBSFPZLNA";
    public static String AMAZONEC2_SECRET_KEY = "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";
    public static String AMAZONEC2_VPC_ID = "vpc-addacac5";
    public static String AMAZONEC2_ZONE = "a";
    public static String AMAZONEC2_SSH_USER = "centos";
    public static String AMAZONEC2_INSTANCE_TYPE = "c3.large";
    public static String AMAZONEC2_AMI = "ami-f312b580";
    public static String AMAZONEC2_SUBNET_ID = "subnet-aedacac6";
    public static String AMAZONEC2_SECURITY_GROUP = "default";
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
    }

    public Config() {
        try {
            this.environment = new EnvironmentManager("dev");
        }catch (InterruptedException e) {
            e.printStackTrace();
        }

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
