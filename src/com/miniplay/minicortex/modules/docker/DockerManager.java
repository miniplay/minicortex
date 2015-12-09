package com.miniplay.minicortex.modules.docker;

import com.miniplay.common.GlobalFunctions;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Docker manager Object
 * Created by ret on 4/12/15.
 */
public class DockerManager {

    protected ElasticBalancer elasticBalancer = null;
    protected Map<String, Object> dockerConfig = null;
    protected Map<String, Object> amazonEC2Config = null;

    /* Application registered containers */
    public volatile ConcurrentHashMap<String, Container> containers = new ConcurrentHashMap<String, Container>();

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
        System.out.println(GlobalFunctions.PREPEND_DOCKER_OUTPUT + "Starting containers load...");
        try {
            String output = GlobalFunctions.getInstance().executeCommand("docker-machine ls");
            ArrayList<String> containersToAdd = new ArrayList<String>();
            String[] SplittedString = output.split("\n");
            for (String line: SplittedString) {
                StringBuilder newLine = new StringBuilder(line);
                // Exclude headers
                if(!line.contains("ACTIVE") || !line.contains("DRIVER")) {
                    for (int i = 0; i < line.length(); i++){
                        char c = line.charAt(i);
                        // Analyze char only if it isn't the last one
                        if((i+1) < line.length()) {
                            // If current char is whitespace and next char isn't replace current char with a pipe
                            if(c == ' ' && line.charAt(i+1) != ' ') {
                                newLine.setCharAt(i,'|');
                            }
                        }
                    }
                    // Sanitize string and create container
                    String sanitizedContainerLine = newLine.toString();
                    sanitizedContainerLine = sanitizedContainerLine.replaceAll("\\s+","");
                    containersToAdd.add(sanitizedContainerLine);
                }
            }

            this.registerContainersFromProcessString(containersToAdd);

        } catch (Exception e) {
            System.out.println(GlobalFunctions.PREPEND_DOCKER_OUTPUT + "EXCEPTION: " + e.getMessage());
            e.printStackTrace();
        }


    }

    private void registerContainersFromProcessString(ArrayList<String> containersToAdd) {
        System.out.println(GlobalFunctions.PREPEND_DOCKER_OUTPUT + "Registering loaded containers");
        for(String processString:containersToAdd) {
            String[] splittedProcessString = processString.split("\\|");
            System.out.println(GlobalFunctions.PREPEND_DOCKER_OUTPUT + "Registering container ["+processString+"]");

            String containerName = null;
            String containerDriver = null;
            String containerStatus = null;
            String containerUrl = null;

            // Handle different "docker-machine ls" cases... @TODO: Review this...
            if(splittedProcessString.length > 4) { // If ACTIVE flag is setted for this container (exclude it)
                containerName = splittedProcessString[0];
                containerDriver = splittedProcessString[2];
                containerStatus = splittedProcessString[3];
                containerUrl = splittedProcessString[4];
            } else if(splittedProcessString.length > 3) { // Running machine with active flag not provided
                containerName = splittedProcessString[0];
                containerDriver = splittedProcessString[1];
                containerStatus = splittedProcessString[2];
                containerUrl = splittedProcessString[3];
            } else { // Stopped machine case
                containerName = splittedProcessString[0];
                containerDriver = splittedProcessString[1];
                containerStatus = splittedProcessString[2];
                containerUrl = null;
            }

            Boolean registerResponse = this.registerContainer(containerName, containerDriver, containerStatus, containerUrl);
            if(registerResponse) {
                System.out.println(GlobalFunctions.PREPEND_DOCKER_OUTPUT + "Registered new container ["+containerName+"]");
            } else {
                System.out.println(GlobalFunctions.PREPEND_DOCKER_OUTPUT + "ERROR registering new container ["+containerName+"]");
            }

        }
    }

    /**
     * Registers a new container into the application
     * @return Boolean
     */
    public Boolean registerContainer(String name, String driver, String status, String url) {
        try {
            Container container = new Container(name, driver, status, url);
            Boolean containerExists = this.containers.get(name) != null;
            if(containerExists) {
                this.containers.replace(name, container);
            } else {
                this.containers.put(name, container);
            }
            return true;
        } catch (Exception e) {
            System.out.println(GlobalFunctions.PREPEND_DOCKER_OUTPUT + e.getMessage());
            return false;
        }
    }
}


//docker-machine create \
//        --driver amazonec2 \
//        --amazonec2-region "eu-west-1" \
//        --amazonec2-access-key "AKIAIFWSTWXF64SKJYZQ" \
//        --amazonec2-secret-key "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" \
//        --amazonec2-vpc-id "vpc-addacac5" \
//        --amazonec2-zone "a" \
//        --amazonec2-instance-type "c3.large" \
//        --amazonec2-subnet-id "subnet-aedacac6" \
//        --amazonec2-security-group "default" \
//        rafa-test