package com.miniplay.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * App Global Functions used at any time from
 * Created by ret on 7/12/15.
 */
public class GlobalFunctions {

    private static GlobalFunctions instance = null;

    public static final String PREPEND_OUTPUT = "> CortexServer: ";
    public static final String PREPEND_DOCKER_OUTPUT = "> Docker: ";

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
        System.out.println(PREPEND_OUTPUT+output);
    }

    public String executeCommand(String command) {

        StringBuffer output = new StringBuffer();

        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
            while ((line = reader.readLine())!= null) {
                output.append(line + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return output.toString();

    }
}
