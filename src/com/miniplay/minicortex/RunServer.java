package com.miniplay.minicortex;

import com.miniplay.minicortex.server.CortexServer;

/**
 * Created by ret on 4/12/15.
 */
public class RunServer {

    public static void main(String[] args) throws Exception {

        try {
            CortexServer cortexServer = new CortexServer();
            cortexServer.run();
        }catch(Exception e) {
            System.out.println("> CortexServer: ERROR starting "+e.getMessage());
        }

    }

}
