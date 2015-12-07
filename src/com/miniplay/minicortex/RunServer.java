package com.miniplay.minicortex;

import com.miniplay.common.GlobalFunctions;
import com.miniplay.minicortex.config.Config;
import com.miniplay.minicortex.config.EnvironmentManager;
import com.miniplay.minicortex.server.CortexServer;

/**
 * Application Main class, we'll start the service from here.
 * Created by ret on 4/12/15.
 */
public class RunServer {

    public static void main(String[] args) throws Exception {

        System.out.println("MiniCortex service starting...");

        // Handle environment arg
        EnvironmentManager env = null;
        if (args.length>0) {
            try {
                env = new EnvironmentManager(args[0]);
            } catch (Exception e) {
                GlobalFunctions.getInstance().printOutput("Invalid environment: "+ args[2] + " using default [dev] ....");
                env = new EnvironmentManager("dev");
                printCLISyntax();
            }
        }

        // Load service config
        Config serviceConfig;
        if(env != null) {
            serviceConfig = new Config(env);
        } else {
            serviceConfig = new Config();
        }

        // Run CortexServer
        try {
            CortexServer cortexServer = new CortexServer(serviceConfig);
            cortexServer.run();
        }catch(Exception e) {
            GlobalFunctions.getInstance().printOutput("ERROR starting "+e.getMessage());
            printCLISyntax();
        }

    }

    protected static void printCLISyntax() {
        System.out.println("\n");
        System.out.println("Accepted command line arguments:");
        System.out.println("    [ string serverEnvironment (dev|prod) ]     ");
        System.out.println("Where:");
        System.out.println("    serverEnvironment     Environment where we want to run the service (default dev)  ");
    }

}
