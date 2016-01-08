package com.miniplay.minicortex.modules.docker;

import com.miniplay.common.CommandExecutor;
import com.miniplay.common.Debugger;
import com.miniplay.common.Stats;
import com.miniplay.minicortex.config.Config;
import com.miniplay.minicortex.config.ConfigManager;
import com.miniplay.minicortex.exceptions.InvalidProvisionParams;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;
import com.miniplay.minicortex.server.CortexServer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Docker manager Object
 * Created by ret on 4/12/15.
 */
public class ContainerManager {

    protected ElasticBalancer elasticBalancer = null;
    public Boolean isLoaded = false;
    public volatile ConcurrentHashMap<String, Container> containers = new ConcurrentHashMap<String, Container>();
    private Config config = null;

    /**
     * ContainerManager constructor
     * @param elasticBalancer ElasticBalancer
     */
    public ContainerManager(ElasticBalancer elasticBalancer) {
        this.elasticBalancer = elasticBalancer;
        this.loadConfig();

        System.out.println(Debugger.PREPEND_OUTPUT + "ContainerManager Loaded OK");
    }

    /**
     * Load conf into obj
     */
    private void loadConfig() {
        this.config = ConfigManager.getConfig();

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
        try {
            String output = CommandExecutor.getInstance().execute("docker-machine ls");
            //System.out.println("Container load: " +output);
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
            System.out.println(Debugger.PREPEND_OUTPUT_DOCKER + "EXCEPTION: " + e.getMessage());
            e.printStackTrace();
        }


    }

    /**
     * Batch register containers into app (from process string extracted from docker-machine ls)
     * @param containersToAdd ArrayList
     */
    private void registerContainersFromProcessString(ArrayList<String> containersToAdd) {
        Debugger.getInstance().debug("Registering loaded containers", this.getClass());
        for(String processString:containersToAdd) {
            try {
                String[] splittedProcessString = processString.split("\\|");
                Debugger.getInstance().debug("Registering container ["+processString+"]", this.getClass());

                if(splittedProcessString[3].equals("Timeout")) {
                    throw new Exception("Container state was timeout, skipping");
                }

                String containerName = splittedProcessString[0].equals("null") ? null : splittedProcessString[0];
                String containerDriver = splittedProcessString[2].equals("null") ? null : splittedProcessString[2];
                String containerState = splittedProcessString[3].equals("null") ? null : splittedProcessString[3];
                String containerUrl = splittedProcessString[4].equals("null") ? null : splittedProcessString[4];

                Boolean registerResponse = this.registerContainer(containerName, containerDriver, containerState, containerUrl);
                if(registerResponse) {
                    Debugger.getInstance().debug("Registered new container ["+containerName+"]", this.getClass());
                } else {
                    System.out.println(Debugger.PREPEND_OUTPUT_DOCKER + "ERROR registering new container ["+containerName+"]");
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
            System.out.println(Debugger.PREPEND_OUTPUT_DOCKER + e.getMessage());
            return false;
        }
    }

    /**
     * Provision a new container into the Docker cluster
     * @param containerName String
     */
    public void provisionContainer(String containerName) throws InvalidProvisionParams {
        try {
            Debugger.getInstance().printOutput("Provisioning new container #"+containerName);
            StringBuilder command = new StringBuilder();

            command.append("docker-machine create ");
            command.append("--driver amazonec2 ");

            if(this.config.AMAZONEC2_REGION != null) {
                String commandPart = "--amazonec2-region '"+this.config.AMAZONEC2_REGION+"' ";
                command.append(commandPart);
            } else {
                throw new InvalidProvisionParams("Invalid AMAZONEC2_REGION provided");
            }

            if(this.config.AMAZONEC2_ACCESS_KEY != null) {
                String commandPart = "--amazonec2-access-key '"+this.config.AMAZONEC2_ACCESS_KEY+"' ";
                command.append(commandPart);
            } else {
                throw new InvalidProvisionParams("Invalid AMAZONEC2_ACCESS_KEY provided");
            }

            if(this.config.AMAZONEC2_SECRET_KEY != null) {
                String commandPart = "--amazonec2-secret-key '"+this.config.AMAZONEC2_SECRET_KEY+"' ";
                command.append(commandPart);
            } else {
                throw new InvalidProvisionParams("Invalid AMAZONEC2_SECRET_KEY provided");
            }

            if(this.config.AMAZONEC2_VPC_ID != null) {
                String commandPart = "--amazonec2-vpc-id '"+this.config.AMAZONEC2_VPC_ID+"' ";
                command.append(commandPart);
            } else {
                throw new InvalidProvisionParams("Invalid AMAZONEC2_VPC_ID provided");
            }

            if(this.config.AMAZONEC2_ZONE != null) {
                String commandPart = "--amazonec2-zone '"+this.config.AMAZONEC2_ZONE+"' ";
                command.append(commandPart);
            } else {
                throw new InvalidProvisionParams("Invalid AMAZONEC2_ZONE provided");
            }

            if(this.config.AMAZONEC2_INSTANCE_TYPE != null) {
                String commandPart = "--amazonec2-instance-type '"+this.config.AMAZONEC2_INSTANCE_TYPE+"' ";
                command.append(commandPart);
            } else {
                throw new InvalidProvisionParams("Invalid AMAZONEC2_INSTANCE_TYPE provided");
            }

            if(this.config.AMAZONEC2_SECURITY_GROUP != null) {
                String commandPart = "--amazonec2-security-group '"+this.config.AMAZONEC2_SECURITY_GROUP+"' ";
                command.append(commandPart);
            } else {
                throw new InvalidProvisionParams("Invalid AMAZONEC2_SECURITY_GROUP provided");
            }

            if(this.config.AMAZONEC2_SSH_USER != null) {
                String commandPart = "--amazonec2-ssh-user '"+this.config.AMAZONEC2_SSH_USER+"' ";
                command.append(commandPart);
            }

            if(this.config.AMAZONEC2_AMI != null) {
                String commandPart = "--amazonec2-ami '"+this.config.AMAZONEC2_AMI+"' ";
                command.append(commandPart);
            }

            if(this.config.AMAZONEC2_SUBNET_ID != null) {
                String commandPart = "--amazonec2-subnet-id '"+this.config.AMAZONEC2_SUBNET_ID+"' ";
                command.append(commandPart);
            }

            if(this.config.AMAZONEC2_PRIVATE_ADDRESS_ONLY) {
                String commandPart = "--amazonec2-private-address-only ";
                command.append(commandPart);
            }

            command.append(containerName);

            Debugger.getInstance().debug("Machine provision command: " + command, this.getClass());
            String creationOutput = CommandExecutor.getInstance().execute(command.toString());
            Debugger.getInstance().debug("Machine provisioned! - "+creationOutput, this.getClass());

            if(Stats.getInstance().isEnabled()) {
                Stats.getInstance().get().increment("minicortex.observers.containers.provision");
            }

        } catch (IOException e) {
            System.out.println(Debugger.PREPEND_OUTPUT_DOCKER + "EXCEPTION: " + e.getMessage());
        }

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
     * @param state Stringr
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

    public void provisionContainers(Integer containersToProvision) {
        for(int i = 1; i<=containersToProvision; i++) {
            Debugger.getInstance().printOutput("Provisioning container "+i+"/"+containersToProvision);
            ProvisionThread provisionThread = new ProvisionThread("ProvisionThread #" + i);
            provisionThread.start();

        }
    }

    public void killContainers(Integer containersToKill) {
        for(int i = 1; i<=containersToKill; i++) {
            Debugger.getInstance().printOutput("Killing container "+i+"/"+containersToKill);
            this.killRandomContainer();

        }
    }

    public void startContainers(Integer containersToStart) {
        for(int i = 1; i<=containersToStart; i++) {
            Debugger.getInstance().printOutput("Starting container "+i+"/"+containersToStart);
            this.startRandomContainer();

        }
    }

    private void killRandomContainer() {
        Container containerToKill = this.getRandomContainer(Container.STATUS_RUNNING);
        containerToKill.kill();
    }

    private void startRandomContainer() {
        Container containerToStart = this.getRandomContainer(Container.STATUS_STOPPED);
        containerToStart.start();
    }

    /**
     * Retrieve a random registered container
     * @param state String
     * @return Container
     */
    private Container getRandomContainer(String state) {
        Random r = new Random();
        ArrayList<Container> containers;
        if(state.equals(Container.STATUS_STOPPED)) {
            containers = this.getStoppedContainers();
        } else if(state.equals(Container.STATUS_RUNNING)) {
            containers = this.getRunningContainers();
        } else {
            containers = this.getAllContainers();
        }
        return containers.get(r.nextInt(containers.size()));
    }

    public Config getConfig() {
        return config;
    }
}