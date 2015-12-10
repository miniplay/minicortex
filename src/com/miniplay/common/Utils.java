package com.miniplay.common;

/**
 * App Global Functions used at any time from
 * Created by ret on 7/12/15.
 */
public class Utils {

    private static Utils instance = null;

    public static final String PREPEND_OUTPUT = "> CortexServer: ";
    public static final String PREPEND_OUTPUT_OBSERVERS = "> Observers: ";
    public static final String PREPEND_OUTPUT_DOCKER = "> ContainerManager: ";

    /**
     * Utils instance (Singleton)
     * @return Utils instance
     */
    public static Utils getInstance(){
        if(instance == null) {
            instance = new Utils();
        }
        return instance;
    }

    public void printOutput(Object output) {
        System.out.println(PREPEND_OUTPUT+output);
    }

}
