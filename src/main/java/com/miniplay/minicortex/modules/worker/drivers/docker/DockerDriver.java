package com.miniplay.minicortex.modules.worker.drivers.docker;

import com.google.common.collect.ImmutableMap;
import com.miniplay.common.Debugger;
import com.miniplay.common.Stats;
import com.miniplay.minicortex.config.Config;
import com.miniplay.minicortex.config.ConfigManager;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;
import com.miniplay.minicortex.modules.worker.drivers.AbstractWorkerDriver;
import com.miniplay.minicortex.modules.worker.exceptions.DriverException;
import com.spotify.docker.client.*;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.*;

import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Docker manager Object
 * Created by ret on 4/12/15.
 */
public class DockerDriver extends AbstractWorkerDriver {

    private DockerClient dockerClient = null;
    protected ElasticBalancer elasticBalancer = null;
    public Boolean isLoaded = false;
    public volatile ConcurrentHashMap<String, Worker> workers = new ConcurrentHashMap<String, Worker>();

    private Config config = null;
    private String dockerEnvironment = null;
    private String dockerServiceName = null;

    /**
     * DockerDriver constructor
     * @param elasticBalancer ElasticBalancer
     */
    public DockerDriver(ElasticBalancer elasticBalancer) throws DriverException {
        super(elasticBalancer);
        this.elasticBalancer = elasticBalancer;


        // Load config & Instance new docker client
        try {

            this.loadConfig();

            if(config.DOCKER_CONNECTION.get("HOST") == null) {
                throw new DriverException("Docker connection host not defined");
            }
            if(config.DOCKER_CONNECTION.get("HOST") != null &&
                    config.DOCKER_CONNECTION.get("HOST").contains("https://") &&
                    config.DOCKER_CONNECTION.get("CERTS_DIR") == null) {
                throw new DriverException("Docker connection host defined but it's certs dir NOT");
            }
            if(config.DOCKER_CONNECTION.get("HOST").contains("unix:///")) { // Use the unix socket way when available
                this.dockerClient = new DefaultDockerClient(config.DOCKER_CONNECTION.get("HOST"));
            } else { // Use https connection way
                this.dockerClient = DefaultDockerClient.builder()
                        .uri(URI.create(config.DOCKER_CONNECTION.get("HOST")))
                        .dockerCertificates(new DockerCertificates(Paths.get(config.DOCKER_CONNECTION.get("CERTS_DIR"))))
                        .build();
            }

            Debugger.getInstance().print("DockerDriver Loaded OK",this.getClass());

        } catch(Exception e) {
            System.out.println("Docker client instance exception: " + e.getMessage());
            throw new DriverException("Docker Client could not initialize. Exiting.");
        }
    }


    /**
     * LOADING ---------------------------------------------------------------------------------------------------------
     */


    /**
     * Load conf into obj
     */
    private void loadConfig() throws DriverException {
        this.config = ConfigManager.getConfig();

        this.setMinWorkers(config.DOCKER_MIN_CONTAINERS);
        this.setMaxWorkers(config.DOCKER_MAX_CONTAINERS);
        this.setMaxBootsInLoop(config.DOCKER_MAX_BOOTS_IN_LOOP);
        this.setMaxShutdownsInLoop(config.DOCKER_MAX_SHUTDOWNS_IN_LOOP);
        this.setDockerEnvironment(config.DOCKER_ENV_VARS);
        this.setDockerServiceName(config.SERVICE_NAME);

        this.isLoaded = true;
    }

    /**
     * Load workers from "docker-machine ls" into application
     */
    public void loadWorkers() {
        try {

            // Load workers filtering by service name
            List<Container> currentContainers = dockerClient.listContainers(
                    DockerClient.ListContainersParam.allContainers(),
                    DockerClient.ListContainersParam.withLabel("environment", this.dockerEnvironment),
                    DockerClient.ListContainersParam.withLabel("service", this.dockerServiceName));

            Debugger.getInstance().print(
                    "Loaded " + currentContainers.size() +
                    " containers matching ENV [" + this.dockerEnvironment + "] and Service [" +
                    this.dockerServiceName + "]",this.getClass());

            for (Container container: currentContainers) {

                // Inspect container to get it's info
                ContainerInfo containerInfo = dockerClient.inspectContainer(container.id());

                String containerId = container.id();
                String containerName = container.names().get(0);
                containerName = containerName.startsWith("/") ? containerName.substring(1) : containerName;
                String containerImage = container.image();
                List<Container.PortMapping> containerBindPorts = container.ports();
                List<String> containerEnvVars = containerInfo.config().env();
                List<ContainerMount> containerMountedVolumes = container.mounts();
                ContainerState containerState = containerInfo.state();

                this.registerWorker(
                        containerId,
                        containerName,
                        containerImage,
                        containerBindPorts,
                        containerEnvVars,
                        containerMountedVolumes,
                        containerState
                );
            }





        } catch (Exception e) {
            System.out.println(Debugger.PREPEND_OUTPUT_DOCKER + "Load Workers Exception: " + e.getMessage());
            e.printStackTrace();
        }


    }


    /**
     * PROVISION & REGISTER --------------------------------------------------------------------------------------------
     */



    public void provisionWorkers(Integer workersToProvision) {
        for(int i = 1; i<=workersToProvision; i++) {
            Debugger.getInstance().printOutput("Provisioning worker "+i+"/"+workersToProvision);

            // Generate secure random string
            SecureRandom random = new SecureRandom();
            String randomString =  new BigInteger(130, random).toString(32);

            // Provision worker with random name
            String workerName = ConfigManager.getConfig().DOCKER_BASENAME + randomString.substring(2,7);

            // @TODO: Check if the workerName already exists (already done in registerWorker but just in case)

            provisionWorker(workerName);

        }
    }

    /**
     * Provision a new worker into the Docker cluster
     * @param workerName String
     */
    public void provisionWorker(String workerName) {
        try {
            Debugger.getInstance().printOutput("Provisioning new worker #"+workerName);

            //Provision new worker

            // Generate configBuilder
            final HostConfig.Builder hostConfigBuilder = HostConfig.builder();

            // Set exposed ports
            final Map<String, List<PortBinding>> portBindings = new HashMap<String, List<PortBinding>>();
            for (String port:this.getConfig().DOCKER_PORTS) {
                String[] splittedPort = port.split(":");
                if(splittedPort[0] != null && splittedPort[1] != null) {
                    portBindings.put(splittedPort[0], Arrays.asList( PortBinding.of( "", splittedPort[1] )));
                }

            }
            hostConfigBuilder.portBindings(portBindings);


            // Set volumes
            for (String volume:this.getConfig().DOCKER_VOLUMES) {
                hostConfigBuilder.appendBinds(volume);
            }

            // set builder properties, including the hostconfig build
            ContainerConfig.Builder containerBuilder = ContainerConfig.builder()
                .image(this.getConfig().DOCKER_IMAGE)
                .env(config.DOCKER_ENV_VARS)
                .labels(ImmutableMap.of("environment",this.dockerEnvironment,"service",this.dockerServiceName))
                .hostConfig(hostConfigBuilder.build())
                .hostname(workerName);

            // create the container
            final ContainerCreation container = this.dockerClient.createContainer(containerBuilder.build(),workerName);

            // get created container information
            ContainerInfo containerInfo = dockerClient.inspectContainer(container.id());

            // @TODO: Register worker with proper PortMappings
            List<Container.PortMapping> portMappings = new ArrayList<Container.PortMapping>();


            this.registerWorker(
                    containerInfo.id(),
                    workerName,
                    containerInfo.image(),
                    portMappings,
                    containerInfo.config().env(),
                    containerInfo.mounts(),
                    containerInfo.state());


            Debugger.getInstance().debug("Worker provisioned! - id -> "+containerInfo.id(), this.getClass());

            if(Stats.getInstance().isEnabled()) {
                Stats.getInstance().get().increment("minicortex.observers.docker.provision");
            }

        } catch (InterruptedException e) {
            Debugger.getInstance().print("Error provisioning worker: " + e.getMessage(), this.getClass());
        } catch (DockerException e) {
            Debugger.getInstance().print("Error provisioning worker: " + e.getMessage(), this.getClass());
        }

    }

    /**
     * Registers a new worker into the application
     * @return Boolean
     */
    public Boolean registerWorker(String id, String name, String image, List<Container.PortMapping> bindPorts, List<String> envVars, List<ContainerMount> mountedVolumes, ContainerState state) {
        try {
            Worker worker = new Worker(this, id, this.dockerClient, name, image, bindPorts, envVars, mountedVolumes, state);
            Boolean workerExists = this.workers.get(name) != null;
            if(workerExists) {
                this.workers.replace(name, worker);
            } else {
                this.workers.put(name, worker);
            }

            return true;
        } catch (Exception e) {
            System.out.println(Debugger.PREPEND_OUTPUT_DOCKER + "Register Workers Exception: " + e.getMessage());
            return false;
        }
    }


    /**
     * ACTIONS ---------------------------------------------------------------------------------------------------------
     */


    public void killWorkers(Integer workersToKill) {
        for(int i = 1; i<=workersToKill; i++) {
            Debugger.getInstance().printOutput("Killing worker "+i+"/"+workersToKill);
            this.killRandomWorker();

        }
    }

    public void startWorkers(Integer workersToStart) {
        for(int i = 1; i<=workersToStart; i++) {
            Debugger.getInstance().printOutput("Starting worker "+i+"/"+workersToStart);
            this.startRandomWorker();

        }
    }

    private void killRandomWorker() {
        Worker workerToKill = this.getRandomWorker(Worker.STATUS_RUNNING);
        if(workerToKill != null) {
            workerToKill.kill();
        } else {
            Debugger.getInstance().print("Didn't found any running container to kill.", this.getClass());
        }
    }

    private void startRandomWorker() {
        Worker workerToStart;
        workerToStart = this.getRandomWorker(Worker.STATUS_EXITED);
        if(workerToStart == null) {
            // Sometimes we don't have EXITED containers, i.e. first run, and we need to use recently created containers
            workerToStart = this.getRandomWorker(Worker.STATUS_CREATED);
        }

        if(workerToStart != null) {
            workerToStart.start();
        } else {
            Debugger.getInstance().print("Didn't found any Exited or Created container to start.", this.getClass());
        }
    }


    /**
     * GETTERS ---------------------------------------------------------------------------------------------------------
     */



    /**
     * Get stopped workers (exited)
     * @return ArrayList
     */
    public ArrayList<Worker> getStoppedWorkers() {
        return this.getWorkers(Worker.STATUS_EXITED);
    }

    /**
     * Get running workers
     * @return ArrayList
     */
    public ArrayList<Worker> getRunningWorkers() {
        return this.getWorkers(Worker.STATUS_RUNNING);
    }

    /**
     * Get paused workers
     * @return ArrayList
     */
    public ArrayList<Worker> getPausedWorkers() {
        return this.getWorkers(Worker.STATUS_PAUSED);
    }

    /**
     * Get created workers
     * @return ArrayList
     */
    public ArrayList<Worker> getCreatedWorkers() {
        return this.getWorkers(Worker.STATUS_CREATED);
    }

    /**
     * Get ALL workers
     * @return ArrayList
     */
    public ArrayList<Worker> getAllWorkers() {
        return this.getWorkers(null);
    }

    /**
     * Get workers by state
     * @param state Stringr
     * @return ArrayList
     */
    private ArrayList<Worker> getWorkers(String state) {
        ArrayList<Worker> matchWorkers = new ArrayList<Worker>();
        for(Map.Entry<String, Worker> entry : this.workers.entrySet()) {
            Worker currentWorker = entry.getValue();
            if(state == null || currentWorker.getState().equals(state)) {
                matchWorkers.add(currentWorker);
            }
        }
        return matchWorkers;
    }

    /**
     * Retrieve a random registered worker
     * @param state String
     * @return Worker
     */
    private Worker getRandomWorker(String state) {
        Random r = new Random();
        ArrayList<Worker> workers;
        if(state.equals(Worker.STATUS_EXITED)) {
            workers = this.getStoppedWorkers();
        } else if(state.equals(Worker.STATUS_RUNNING)) {
            workers = this.getRunningWorkers();
        } else if(state.equals(Worker.STATUS_CREATED)) {
            workers = this.getCreatedWorkers();
        } else {
            workers = this.getAllWorkers();
        }
        if(workers.size()>=1) {
            return workers.get(r.nextInt(workers.size()));
        } else {
            return null;
        }
    }

    /**
     * @return Config
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Get registered worker by worker name
     * @param name String
     * @return Worker
     */
    public Worker getWorker(String name) {
        return this.workers.get(name);
    }



    /**
     * SETTERS ---------------------------------------------------------------------------------------------------------
     */


    public void setDockerEnvironment(ArrayList<String> dockerEnvironment) throws DriverException {

        for(int i = 0; i< dockerEnvironment.size(); i++) {
            if(dockerEnvironment.get(i).contains("ENV")) {
                String[] parts = dockerEnvironment.get(i).split("=");
                this.dockerEnvironment = parts[1];
            }
        }
        if(this.dockerEnvironment == null) {
            throw new DriverException("Docker ENV var not found in DOCKER_ENV_VARS");
        }
    }

    public void setDockerServiceName(String dockerServiceName) {
        this.dockerServiceName = dockerServiceName;
    }

}