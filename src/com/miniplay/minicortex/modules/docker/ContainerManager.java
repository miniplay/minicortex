package com.miniplay.minicortex.modules.docker;

import com.miniplay.common.CommandExecutor;
import com.miniplay.common.Utils;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;
import com.miniplay.minicortex.server.CortexServer;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Docker manager Object
 * Created by ret on 4/12/15.
 */
public class ContainerManager {

    protected ElasticBalancer elasticBalancer = null;
    protected Map<String, Object> dockerConfig = null;
    protected Map<String, Object> amazonEC2Config = null;
    public Boolean isLoaded = false;

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
    public Boolean AMAZONEC2_PRIVATE_ADDRESS_ONLY = null;

    /**
     * ContainerManager constructor
     * @param elasticBalancer ElasticBalancer
     * @param dockerConfig Map
     * @param amazonEC2Config Map
     */
    public ContainerManager(ElasticBalancer elasticBalancer, Map<String, Object> dockerConfig, Map<String, Object> amazonEC2Config) {
        // Load elastic balancer instance
        this.elasticBalancer = elasticBalancer;

        // Load docker & EC2 config
        this.dockerConfig = dockerConfig;
        this.amazonEC2Config = amazonEC2Config;

        this.loadConfig();


        System.out.println(Utils.PREPEND_OUTPUT + "ContainerManager Loaded OK");
    }

    /**
     * TODO: Load config from file
     */
    private void loadConfig() {
        this.isLoaded = true;
    }

    /**
     * Get registered container by container name
     * @param name String
     * @return Container
     */
    public Container getContainerByName(String name) {
        return this.containers.get(name);
    }

    /**
     * Load containers from "docker-machine ls" into application
     */
    public void loadContainers() {
        if(CortexServer.DEBUG) System.out.println(Utils.PREPEND_OUTPUT_DOCKER + "Starting containers load...");
        try {
            String output = CommandExecutor.getInstance().execute("docker-machine ls");
            ArrayList<String> containersToAdd = new ArrayList<String>();
            String[] SplittedString = output.split("\n");
            for (String line: SplittedString) {
                // Exclude headers & error lines
                if((!line.contains("ACTIVE") && !line.contains("DRIVER")) && !line.toLowerCase().contains("error")) {
                    // Sanitize string and create container
                    line = line.replaceAll("\\s{12}","|null|"); // Replace unexisting fields (docker-machine uses spaces)
                    line = line.replaceAll("\\s{3}","|"); // Replace docker tabs with pipes
                    line = line.trim().replaceAll("\\|\\|+", "\\|"); // Replace >1 pipes with a single one
                    line = line.replaceAll("\\s+","");
                    containersToAdd.add(line);
                }
            }

            this.registerContainersFromProcessString(containersToAdd);

        } catch (Exception e) {
            System.out.println(Utils.PREPEND_OUTPUT_DOCKER + "EXCEPTION: " + e.getMessage());
            e.printStackTrace();
        }


    }

    /**
     * Batch register containers into app (from process string extracted from docker-machine ls)
     * @param containersToAdd ArrayList
     */
    private void registerContainersFromProcessString(ArrayList<String> containersToAdd) {
        if(CortexServer.DEBUG) System.out.println(Utils.PREPEND_OUTPUT_DOCKER + "Registering loaded containers");
        for(String processString:containersToAdd) {
            try {
                String[] splittedProcessString = processString.split("\\|");
                if(CortexServer.DEBUG) System.out.println(Utils.PREPEND_OUTPUT_DOCKER + "Registering container ["+processString+"]");

                if(splittedProcessString[3].equals("Timeout")) {
                    throw new Exception("Container state was timeout, skipping");
                }

                String containerName = splittedProcessString[0].equals("null") ? null : splittedProcessString[0];
                String containerDriver = splittedProcessString[2].equals("null") ? null : splittedProcessString[2];
                String containerState = splittedProcessString[3].equals("null") ? null : splittedProcessString[3];
                String containerUrl = splittedProcessString[4].equals("null") ? null : splittedProcessString[4];

                Boolean registerResponse = this.registerContainer(containerName, containerDriver, containerState, containerUrl);
                if(registerResponse) {
                    if(CortexServer.DEBUG) System.out.println(Utils.PREPEND_OUTPUT_DOCKER + "Registered new container ["+containerName+"]");
                } else {
                    System.out.println(Utils.PREPEND_OUTPUT_DOCKER + "ERROR registering new container ["+containerName+"]");
                }
            } catch (Exception e) {
                System.out.println("Exception registering container [" + processString + "] message: " + e.getMessage());
            }
        }
    }

    /**
     * Registers a new container into the application
     * @return Boolean
     */
    public Boolean registerContainer(String name, String driver, String state, String url) {
        try {
            Container container = new Container(name, driver, state, url);
            Boolean containerExists = this.containers.get(name) != null;
            if(containerExists) {
                this.containers.replace(name, container);
            } else {
                this.containers.put(name, container);
            }
            return true;
        } catch (Exception e) {
            System.out.println(Utils.PREPEND_OUTPUT_DOCKER + e.getMessage());
            return false;
        }
    }

    /**
     * Provision a new container into the Docker cluster
     * @param containerName String
     */
    public void provisionContainer(String containerName) {
        String creationOutput = CommandExecutor.getInstance().execute(
            "docker-machine create \\" +
            "--driver amazonec2 \\" +
            "--amazonec2-region '"+this.AMAZONEC2_REGION+"' \\" +
            "--amazonec2-access-key '"+this.AMAZONEC2_ACCESS_KEY+"' \\" +
            "--amazonec2-secret-key '"+this.AMAZONEC2_SECRET_KEY+"' \\" +
            "--amazonec2-vpc-id '"+this.AMAZONEC2_VPC_ID+"' \\" +
            "--amazonec2-zone '"+this.AMAZONEC2_ZONE+"' \\" +
            "--amazonec2-ssh-user '"+this.AMAZONEC2_SSH_USER+"' \\" +
            "--amazonec2-instance-type '"+this.AMAZONEC2_INSTANCE_TYPE+"' \\" +
            "--amazonec2-ami '"+this.AMAZONEC2_AMI+"' \\" +
            "--amazonec2-subnet-id '"+this.AMAZONEC2_SUBNET_ID+"' \\" +
            "--amazonec2-security-group '"+this.AMAZONEC2_SECURITY_GROUP+"' \\" +
            "--amazonec2-use-private-address \\" +
            "--amazonec2-private-address-only "+this.AMAZONEC2_PRIVATE_ADDRESS_ONLY+" \\" +
            containerName
        );

    }

    /**
     * Get stopped containers
     * @return ArrayList
     */
    public ArrayList<Container> getStoppedContainers() {
        return this.getContainers(Container.STATUS_STOPPED);
    }

    /**
     * Get ERROR containers
     * @return ArrayList
     */
    public ArrayList<Container> getErrorContainers() {
        return this.getContainers(Container.STATUS_ERROR);
    }

    /**
     * Get running containers
     * @return ArrayList
     */
    public ArrayList<Container> getRunningContainers() {
        return this.getContainers(Container.STATUS_RUNNING);
    }

    /**
     * Get stopping containers
     * @return ArrayList
     */
    public ArrayList<Container> getStoppingContainers() {
        return this.getContainers(Container.STATUS_STOPPING);
    }

    /**
     * Get starting containers
     * @return ArrayList
     */
    public ArrayList<Container> getStartingContainers() {
        return this.getContainers(Container.STATUS_STARTING);
    }

    /**
     * Get ALL containers
     * @return ArrayList
     */
    public ArrayList<Container> getAllContainers() {
        return this.getContainers(null);
    }

    /**
     * Get containers by state
     * @param state String
     * @return ArrayList
     */
    private ArrayList<Container> getContainers(String state) {
        ArrayList<Container> matchContainers = new ArrayList<Container>();
        for(Map.Entry<String, Container> entry : this.containers.entrySet()) {
            Container currentContainer = entry.getValue();
            if(state == null || currentContainer.getState().equals(state)) {
                matchContainers.add(currentContainer);
            }
        }
        return matchContainers;
    }

}