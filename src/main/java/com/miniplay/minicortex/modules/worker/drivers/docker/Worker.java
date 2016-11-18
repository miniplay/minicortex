package com.miniplay.minicortex.modules.worker.drivers.docker;

import com.miniplay.common.CommandExecutor;
import com.miniplay.common.Debugger;
import com.miniplay.common.Stats;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;
import com.miniplay.minicortex.modules.worker.drivers.AbstractWorker;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerMount;
import com.spotify.docker.client.messages.ContainerState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
    protected List<Container.PortMapping> bindPorts = null;
    protected List<String> envVars;
    protected List<ContainerMount> mountedVolumes;

    private static final int SECS_BEFORE_SIGKILL_CONTAINER = 3600;

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
    public Worker(DockerDriver driverInstance, String id, DockerClient dockerClient, String name, String image, List<Container.PortMapping> bindPorts, List<String> envVars, List<ContainerMount> mountedVolumes, ContainerState state) {
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
            Debugger.getInstance().print("Docker Worker state not recognized, setting CREATED as default state. Received state:"
                    + state.toString(),this.getClass());
        }

        this.dockerClient = dockerClient;


    }


    /**
     * Worker actions---------------------------------------------------------------------------------------------------
     */

    public void start() {
        Debugger.getInstance().print("Worker #" + this.getName() + " is going to start...",this.getClass());

        // Actually start the worker
        try {
            dockerClient.startContainer(this.getName());

            Debugger.getInstance().print("Worker #" + this.getName() + " Started successfully",this.getClass());

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
        Debugger.getInstance().print("Worker #" + this.getName() + " sending SIGTERM to kill it, after " + SECS_BEFORE_SIGKILL_CONTAINER + " seconds we'll send SIGKILL",this.getClass());

        // Actually kill the worker
        try {
            //dockerClient.killContainer(this.getName());
            dockerClient.stopContainer(this.getName(),SECS_BEFORE_SIGKILL_CONTAINER);

            Debugger.getInstance().print("Worker #" + this.getName() + " Killed successfully",this.getClass());

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

            Debugger.getInstance().print("Worker #" + this.getName() + " Removed successfully",this.getClass());

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
     * @return List<Container.PortMapping>
     */
    public List<Container.PortMapping> getBindPorts() {
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
     * @param bindPorts List<Container.PortMapping>
     */
    public void setBindPorts(List<Container.PortMapping> bindPorts) {
        this.bindPorts = bindPorts;
    }
}
