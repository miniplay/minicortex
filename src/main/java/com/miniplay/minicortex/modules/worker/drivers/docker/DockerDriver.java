package com.miniplay.minicortex.modules.worker.drivers.docker;

import com.miniplay.common.CommandExecutor;
import com.miniplay.common.Debugger;
import com.miniplay.common.Stats;
import com.miniplay.minicortex.config.Config;
import com.miniplay.minicortex.config.ConfigManager;
import com.miniplay.minicortex.exceptions.InvalidProvisionParams;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;
import com.miniplay.minicortex.modules.worker.drivers.AbstractWorkerDriver;
import com.miniplay.minicortex.modules.worker.drivers.dockermachine.ProvisionThread;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificateException;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.ContainerState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
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

    /**
     * DockerMachineDriver constructor
     * @param elasticBalancer ElasticBalancer
     */
    public DockerDriver(ElasticBalancer elasticBalancer) {
        super(elasticBalancer);
        this.elasticBalancer = elasticBalancer;
        this.loadConfig();

        // Instance new docker client
        try {
            this.dockerClient = DefaultDockerClient.fromEnv().build();
        } catch(DockerCertificateException e) {
            System.out.println("Docker client instance exception: " + e.getMessage());
        }

        System.out.println(Debugger.PREPEND_OUTPUT + "DockerMachineDriver Loaded OK");
    }

    /**
     * Load conf into obj
     */
    private void loadConfig() {
        this.config = ConfigManager.getConfig();

        this.setMinWorkers(config.DOCKER_MIN_CONTAINERS);
        this.setMaxWorkers(config.DOCKER_MAX_CONTAINERS);
        this.setMaxBootsInLoop(config.DOCKER_MAX_BOOTS_IN_LOOP);
        this.setMaxShutdownsInLoop(config.DOCKER_MAX_SHUTDOWNS_IN_LOOP);

        this.isLoaded = true;
    }

    /**
     * Get registered worker by worker name
     * @param name String
     * @return Worker
     */
    public Worker getWorkerByName(String name) {
        return this.workers.get(name);
    }

    /**
     * Load workers from "docker-machine ls" into application
     */
    public void loadWorkers() {
        try {

            // @TODO: Load workers


        } catch (Exception e) {
            System.out.println(Debugger.PREPEND_OUTPUT_DOCKER + "EXCEPTION: " + e.getMessage());
            e.printStackTrace();
        }


    }

    /**
     * Registers a new worker into the application
     * @return Boolean
     */
    public Boolean registerWorker(String id, String name, String image, String bindPorts, ContainerState state) {
        try {
            Worker worker = new Worker(this, id, this.dockerClient, name, image, bindPorts, state);
            Boolean workerExists = this.workers.get(name) != null;
            if(workerExists) {
                this.workers.replace(name, worker);
            } else {
                this.workers.put(name, worker);
            }

            return true;
        } catch (Exception e) {
            System.out.println(Debugger.PREPEND_OUTPUT_DOCKER + e.getMessage());
            return false;
        }
    }

    /**
     * Provision a new worker into the Docker cluster
     * @param workerName String
     */
    public void provisionWorker(String workerName) throws InvalidProvisionParams {
        try {
            Debugger.getInstance().printOutput("Provisioning new worker #"+workerName);

            //Provision new worker (docker container run?)

            final ContainerCreation container = this.dockerClient.createContainer(ContainerConfig.builder()
                    .image(this.getConfig().DOCKER_IMAGE)
                    .hostname(workerName)
                    .exposedPorts(this.getConfig().DOCKER_PORTS)
                    .build(),workerName);

            ContainerInfo containerInfo = dockerClient.inspectContainer(container.id());


            this.registerWorker(container.id(), workerName, this.getConfig().DOCKER_IMAGE, this.getConfig().DOCKER_PORTS,containerInfo.state());


            Debugger.getInstance().debug("Worker provisioned! - id -> "+container.id(), this.getClass());

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

    public void provisionWorkers(Integer workersToProvision) {
        for(int i = 1; i<=workersToProvision; i++) {
            Debugger.getInstance().printOutput("Provisioning worker "+i+"/"+workersToProvision);
            ProvisionThread provisionThread = new ProvisionThread("ProvisionThread #" + i);
            provisionThread.start();

        }
    }

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
        workerToKill.kill();
    }

    private void startRandomWorker() {
        Worker workerToStart = this.getRandomWorker(Worker.STATUS_EXITED);
        workerToStart.start();
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
        } else {
            workers = this.getAllWorkers();
        }
        return workers.get(r.nextInt(workers.size()));
    }

    public Config getConfig() {
        return config;
    }
}