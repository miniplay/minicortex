package com.miniplay.minicortex.modules.worker.drivers.docker;

import com.miniplay.common.CommandExecutor;
import com.miniplay.common.Debugger;
import com.miniplay.common.Stats;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;
import com.miniplay.minicortex.modules.worker.drivers.AbstractWorker;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Worker extends AbstractWorker {

    /* STATUSES */
    public static final String STATUS_CREATED = "created";
    public static final String STATUS_RESTARTING = "restarting";
    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_PAUSED = "paused";
    public static final String STATUS_EXITED = "exited";

    /* ACTIONS */
    private static final String ACTION_START = "start";
    private static final String ACTION_KILL = "kill";
    private static final String ACTION_RM = "rm";
    private static final String ACTION_CREATE = "create";
    private static final String ACTION_RUN = "run";


    /* CONTAINER PROPERTIES */
    protected String id = null;
    protected String name = null;
    protected String image = null;
    protected String state = null;
    protected ArrayList<String> bindPorts = null;
    protected ArrayList<String> envVars;
    protected ArrayList<String> mountedVolumes;

    // Docker client to interact with the API
    private DockerClient dockerClient = null;

    // Parent driver instance
    private final DockerDriver driverInstance;


    /**
     * Worker Constructor
     * @param id String
     * @param dockerClient DockerClient
     * @param name String
     * @param image String
     * @param bindPorts String
     * @param state ContainerState
     */
    public Worker(DockerDriver driverInstance, String id, DockerClient dockerClient, String name, String image, ArrayList<String> bindPorts, ArrayList<String> envVars, ArrayList<String> mountedVolumes, ContainerState state) {
        super(name,state.toString());
        this.id = id;
        this.driverInstance = driverInstance;
        this.name = name;
        this.image = image;
        this.bindPorts = bindPorts;
        this.envVars = envVars;
        this.mountedVolumes = mountedVolumes;


        if(state.running()) {
            this.setState(STATUS_RUNNING);
        } else if(state.paused()) {
            this.setState(STATUS_PAUSED);
        } else if(state.restarting()) {
            this.setState(STATUS_RESTARTING);
        } else if(state.exitCode() == 2) {
            this.setState(STATUS_EXITED);
        } else {
            this.setState(STATUS_CREATED);
            Debugger.getInstance().print("Docker Worker state not recognized!! State:" + state.toString(),this.getClass());
        }


        this.dockerClient = dockerClient;


    }


    /**
     * Worker actions------------------------------------------------------------------------------------------------
     */

    public void start() {
        Debugger.getInstance().print("Worker #" + this.getName() + " is going to start...",this.getClass());

        // Actually start the worker
        try {
            dockerClient.startContainer(this.getName());

            if(Stats.getInstance().isEnabled()) {
                Stats.getInstance().get().increment("minicortex.elastic_balancer.workers.worker.start");
            }

        } catch (DockerException e) {
            Debugger.getInstance().print("Worker start error: " + e.getMessage(), this.getClass());
        } catch (InterruptedException e) {
            Debugger.getInstance().print("Worker start error: " + e.getMessage(), this.getClass());
        }


    }

    /**
     * Kill worker (stop)
     */
    public void kill() {
        Debugger.getInstance().print("Worker #" + this.getName() + " is being killed",this.getClass());

        // Actually kill the worker
        try {
            dockerClient.killContainer(this.getName());

            if(Stats.getInstance().isEnabled()) {
                Stats.getInstance().get().increment("minicortex.elastic_balancer.workers.worker.hardkill");
            }

        } catch (DockerException e) {
            Debugger.getInstance().print("Worker kill error: " + e.getMessage(), this.getClass());
        } catch (InterruptedException e) {
            Debugger.getInstance().print("Worker kill error: " + e.getMessage(), this.getClass());
        }

    }

    /**
     * Remove worker
     */
    public void remove() {
        try {
            this.dockerClient.removeContainer(this.getName());

            if(Stats.getInstance().isEnabled()) {
                Stats.getInstance().get().increment("minicortex.elastic_balancer.workers.worker.remove");
            }

        } catch (DockerException e) {
            Debugger.getInstance().print("Worker remove error: " + e.getMessage(), this.getClass());
        } catch (InterruptedException e) {
            Debugger.getInstance().print("Worker remove error: " + e.getMessage(), this.getClass());
        }
    }


    /**
     * Getters ---------------------------------------------------------------------------------------------------------
     */

    /**
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * @return String
     */
    public String getState() {
        return state;
    }

    /**
     * @return String
     */
    public String getId() {
        return id;
    }

    /**
     * @return String
     */
    public String getImage() {
        return image;
    }

    /**
     * @return HashMap
     */
    public ArrayList<String> getBindPorts() {
        return bindPorts;
    }

    /**
     * Setters ---------------------------------------------------------------------------------------------------------
     */

    /**
     * @param state String
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * @param name String
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param id String
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @param image String
     */
    public void setImage(String image) {
        this.image = image;
    }


    /**
     * @param bindPorts ArrayList
     */
    public void setBindPorts(ArrayList<String> bindPorts) {
        this.bindPorts = bindPorts;
    }
}
