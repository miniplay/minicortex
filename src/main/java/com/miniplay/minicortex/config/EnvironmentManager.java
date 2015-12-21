package com.miniplay.minicortex.config;

public class EnvironmentManager {
        
    protected String[] rawArgs;

    protected String environment = "dev";

    public EnvironmentManager(String env) throws InterruptedException {
       if (env.equals("prod")) {
           environment="prod";
       }
       // System.out.println("> Environment Initialized: "+environment);
    }

    public EnvironmentManager(String[] args) throws InterruptedException {
       if (args==null) args = new String[0];
       rawArgs = args;
       if (args.length>0 && args[0].equals("prod")) {
           environment="prod";
       }
       // System.out.println("> Environment Initialized: "+environment);
    }

    public String get() {
       return environment;
    }

    public boolean isDev() {
       return !isProd();
    }

    public boolean isProd() {
       return environment.equals("prod");
    }

    public String getEnvironmentName() {
        return environment;
    }

}
