package com.miniplay.minicortex.modules.docker;

import com.miniplay.common.GlobalFunctions;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;
import com.sun.corba.se.impl.resolver.SplitLocalResolverImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Docker manager Object
 * Created by ret on 4/12/15.
 */
public class DockerManager {

    protected ElasticBalancer elasticBalancer = null;
    protected Map<String, Object> dockerConfig = null;
    protected Map<String, Object> amazonEC2Config = null;

    /* DOCKER */
    public String DOCKER_DEFAULT_DRIVER = null;
    public Integer DOCKER_MIN_CONTAINERS = null;
    public Integer DOCKER_MAX_CONTAINERS = null;
    public Integer DOCKER_MAX_BOOTS_IN_LOOP = null;
    public Integer DOCKER_MAX_SHUTDOWNS_IN_LOOP = null;
    public Boolean DOCKER_TERMINATE_MODE = null;

    /* AMAZON EC2 DOCKER DRIVER */
    public String AMAZONEC2_REGION = null;
    public String AMAZONEC2_ACCESS_KEY = null;
    public String AMAZONEC2_SECRET_KEY = null;
    public String AMAZONEC2_VPC_ID = null;
    public String AMAZONEC2_ZONE = null;
    public String AMAZONEC2_SSH_USER = null;
    public String AMAZONEC2_INSTANCE_TYPE = null;
    public String AMAZONEC2_AMI = null;
    public String AMAZONEC2_SUBNET_ID = null;
    public String AMAZONEC2_SECURITY_GROUP = null;
    public Boolean AMAZONEC2_USE_PRIVATE_ADDRESS = null;
    public Boolean AMAZONEC2_PRIVATE_ADDRESS_ONYL = null;

    /**
     * DockerManager constructor
     * @param elasticBalancer ElasticBalancer
     * @param dockerConfig Map
     * @param amazonEC2Config Map
     */
    public DockerManager(ElasticBalancer elasticBalancer, Map<String, Object> dockerConfig, Map<String, Object> amazonEC2Config) {
        // Load elastic balancer instance
        this.elasticBalancer = elasticBalancer;

        // Load docker & EC2 config
        this.dockerConfig = dockerConfig;
        this.amazonEC2Config = amazonEC2Config;


    }

    private void loadConfig() {

    }

    public void loadContainers() {
        try {
//            String output = GlobalFunctions.getInstance().executeCommand("docker-machine ls");
//            String[] SplittedString = output.split("\n");
//            for (String line: SplittedString) {
//                if(!line.contains("DRIVER")) {
//                    StringBuilder newLine = new StringBuilder(line);
//                    for (int i = 0; i < line.length(); i++){
//                        char c = line.charAt(i);
//                        //System.out.println("Analyzing char: ["+c+"]");
//                        if(c == ' ' && line.charAt(i+1) != ' ') {
//                            //System.out.println("Replacing with | char: "+(i+1));
//                            newLine.setCharAt(i+1,'|');
//                        }
//                    }
//                    System.out.println(newLine);
//                }
//            }
        } catch (Exception e) {
            System.out.println(GlobalFunctions.PREPEND_DOCKER_OUTPUT + e.getMessage());
        }


    }
}
