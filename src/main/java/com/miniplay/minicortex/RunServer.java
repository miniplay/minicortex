package com.miniplay.minicortex;

import com.miniplay.common.Debugger;
import com.miniplay.minicortex.config.Config;
import com.miniplay.minicortex.config.ConfigManager;
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
            env = new EnvironmentManager(args[0]);
        } else {
            env = new EnvironmentManager();
        }

        // Load service config
        ConfigManager.loadConfig(env);

        // Run CortexServer
        try {
            CortexServer cortexServer = new CortexServer();
            cortexServer.run();
        }catch(Exception e) {
            Debugger.getInstance().printOutput("ERROR starting "+e.getMessage());
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
