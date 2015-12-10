package com.miniplay.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 *
 * Created by ret on 9/12/15.
 */
public class CommandExecutor {

    private static CommandExecutor instance = null;

    /**
     * CommandExecutor instance (Singleton)
     * @return CommandExecutor instance
     */
    public static CommandExecutor getInstance(){
        if(instance == null) {
            instance = new CommandExecutor();
        }
        return instance;
    }

    public String execute(String command) {

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
