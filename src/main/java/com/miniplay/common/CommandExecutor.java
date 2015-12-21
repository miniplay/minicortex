package com.miniplay.common;

import java.io.BufferedReader;
import java.io.IOException;
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

    public String execute(String command) throws IOException {

        StringBuffer output = new StringBuffer();

        Process p;
        try {
            String[] commandPlusArgs = new String[]{ "/bin/sh", "-c", command };
            p = Runtime.getRuntime().exec(commandPlusArgs);
            p.waitFor();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
            while ((line = reader.readLine())!= null) {
                output.append(line + "\n");
            }

        } catch (Exception e) {
            if(e instanceof IOException) {
                if(e.getMessage().contains("No such file or directory")) {
                    throw new IOException(e);
                }
            }
            e.printStackTrace();
        }

        return output.toString();

    }
}
