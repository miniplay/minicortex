package com.miniplay.common;

import com.miniplay.minicortex.config.ConfigManager;

/**
 * App Global Functions used at any time from
 * Created by ret on 7/12/15.
 */
public class Debugger {

    private static Debugger instance = null;

    public static final String PREPEND_OUTPUT = "> CortexServer: ";
    public static final String PREPEND_OUTPUT_OBSERVERS = "> Observers: ";
    public static final String PREPEND_OUTPUT_DOCKER = "> ContainerManager: ";
    public static final String PREPEND_OUTPUT_STATUS_CONTAINERS = "> Containers Status: ";

    /**
     * Utils instance (Singleton)
     * @return Utils instance
     */
    public static Debugger getInstance(){
        if(instance == null) {
            instance = new Debugger();
        }
        return instance;
    }

    public void printOutput(Object output) {
        System.out.println(PREPEND_OUTPUT+output);
    }

    public void print(Object message, Class<?> context) {
        if (ConfigManager.getConfig().isDebug()) {
            System.out.println("[" + context.getName() + "]: " + message);
        }
    }

}
