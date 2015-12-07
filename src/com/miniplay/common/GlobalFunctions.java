package com.miniplay.common;

/**
 * App Global Functions used at any time from
 * Created by ret on 7/12/15.
 */
public class GlobalFunctions {

    private static GlobalFunctions instance = null;

    /**
     * GlobalFunctions instance (Singleton)
     * @return GlobalFunctions instance
     */
    public static GlobalFunctions getInstance(){
        if(instance == null) {
            instance = new GlobalFunctions();
        }
        return instance;
    }

    public void printOutput(Object output) {
        System.out.println("> CortexServer: "+output);
    }
}
