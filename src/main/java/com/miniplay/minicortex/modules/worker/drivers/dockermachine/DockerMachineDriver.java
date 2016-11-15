package com.miniplay.minicortex.modules.worker.drivers.dockermachine;

import com.miniplay.common.CommandExecutor;
import com.miniplay.common.Debugger;
import com.miniplay.common.Stats;
import com.miniplay.minicortex.config.Config;
import com.miniplay.minicortex.config.ConfigManager;
import com.miniplay.minicortex.exceptions.InvalidProvisionParams;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;
import com.miniplay.minicortex.modules.worker.drivers.AbstractWorker;
import com.miniplay.minicortex.modules.worker.drivers.AbstractWorkerDriver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Docker manager Object
 * Created by ret on 4/12/15.
 */
public class DockerMachineDriver extends AbstractWorkerDriver {

    protected ElasticBalancer elasticBalancer = null;
    public Boolean isLoaded = false;
    public volatile ConcurrentHashMap<String, Worker> workers = new ConcurrentHashMap<String, Worker>();

    public volatile ConcurrentHashMap<String, Worker> workersScheduledStop = new ConcurrentHashMap<String, Worker>();
    public volatile ConcurrentHashMap<String, Worker> workersScheduledStart = new ConcurrentHashMap<String, Worker>();
    private Config config = null;

    /**
     * DockerMachineDriver constructor
     * @param elasticBalancer ElasticBalancer
     */
    public DockerMachineDriver(ElasticBalancer elasticBalancer) {
        super(elasticBalancer);
        this.elasticBalancer = elasticBalancer;
        this.loadConfig();

        System.out.println(Debugger.PREPEND_OUTPUT + "DockerMachineDriver Loaded OK");
    }

    /**
     * Load conf into obj
     */
    private void loadConfig() {
        this.config = ConfigManager.getConfig();

        this.setMinWorkers(config.DOCKER_MACHINE_MIN_CONTAINERS);
        this.setMaxWorkers(config.DOCKER_MACHINE_MAX_CONTAINERS);
        this.setMaxBootsInLoop(config.DOCKER_MACHINE_MAX_BOOTS_IN_LOOP);
        this.setMaxShutdownsInLoop(config.DOCKER_MACHINE_MAX_SHUTDOWNS_IN_LOOP);

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
            String output = CommandExecutor.getInstance().execute("docker-machine ls");
            System.out.println("Worker load: " +output);
            ArrayList<String> workersToAdd = new ArrayList<String>();
            String[] splittedString = output.split("\n");
            for (String line: splittedString) {
                // Exclude headers & error lines
                if((!line.contains("ACTIVE") && !line.contains("DRIVER")) && !line.toLowerCase().contains("error")) {
                    // Sanitize string and create worker
                    line = line.replaceAll("\\s{12}","|null|"); // Replace unexisting fields (docker-machine uses spaces)
                    line = line.replaceAll("\\s{3}","|"); // Replace docker tabs with pipes
                    line = line.trim().replaceAll("\\|\\|+", "\\|"); // Replace >1 pipes with a single one
                    line = line.replaceAll("\\s+","");
                    workersToAdd.add(line);
                }
            }

            this.registerWorkersFromProcessString(workersToAdd);

        } catch (Exception e) {
            System.out.println(Debugger.PREPEND_OUTPUT_DOCKER + "EXCEPTION: " + e.getMessage());
            e.printStackTrace();
        }


    }

    /**
     * Batch register workers into app (from process string extracted from docker-machine ls)
     * @param workersToAdd ArrayList
     */
    private void registerWorkersFromProcessString(ArrayList<String> workersToAdd) {
        Integer successWorkers = 0;
        Integer errorWorkers = 0;
        Debugger.getInstance().debug("Registering loaded workers", this.getClass());
        for(String processString:workersToAdd) {
            try {
                String[] splittedProcessString = processString.split("\\|");

                if(splittedProcessString[3].equals("Timeout")) {
                    throw new Exception("Worker state was timeout, skipping");
                }

                String workerName = splittedProcessString[0].equals("null") ? null : splittedProcessString[0];
                String workerDriver = splittedProcessString[2].equals("null") ? null : splittedProcessString[2];
                String workerState = splittedProcessString[3].equals("null") ? null : splittedProcessString[3];
                String workerUrl = splittedProcessString[4].equals("null") ? null : splittedProcessString[4];

                Boolean registerResponse = this.registerWorker(workerName, workerDriver, workerState, workerUrl);
                if(registerResponse) {
                    successWorkers++;
                } else {
                    errorWorkers++;
                    System.out.println(Debugger.PREPEND_OUTPUT_DOCKER + "ERROR registering new worker ["+workerName+"]");
                }
            } catch (Exception e) {
                errorWorkers++;
                System.out.println("Exception registering worker [" + processString + "] message: " + e.getMessage());
            }
        }
        Debugger.getInstance().debug("Registered "+successWorkers+" workers with "+errorWorkers+" errors", this.getClass());
    }

    /**
     * Registers a new worker into the application
     * @return Boolean
     */
    public Boolean registerWorker(String name, String driver, String state, String url) {
        try {
            Worker worker = new Worker(this,name, driver, state, url);
            Boolean workerExists = this.workers.get(name) != null;
            if(workerExists) {
                this.workers.replace(name, worker);
            } else {
                this.workers.put(name, worker);
            }

            // remove from scheduled maps if start/stop action is complete and they exist in the scheduled map
            if(this.workersScheduledStart.get(name) != null && worker.getState().equals(Worker.STATUS_RUNNING)) {
                Debugger.getInstance().debug("Worker ["+name+"] existed in workersScheduledStart, removing...", this.getClass());
                this.workersScheduledStart.remove(name);
            }
            if(this.workersScheduledStop.get(name) != null && worker.getState().equals(Worker.STATUS_STOPPED)) {
                Debugger.getInstance().debug("Worker ["+name+"] existed in workersScheduledStop, removing...", this.getClass());
                this.workersScheduledStop.remove(name);
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
            StringBuilder command = new StringBuilder();

            command.append("docker-machine create ");
            command.append("--driver amazonec2 ");

            if(this.config.DOCKER_MACHINE_DRIVER_AMAZONEC2_REGION != null) {
                String commandPart = "--amazonec2-region '"+this.config.DOCKER_MACHINE_DRIVER_AMAZONEC2_REGION+"' ";
                command.append(commandPart);
            } else {
                throw new InvalidProvisionParams("Invalid DOCKER_MACHINE_DRIVER_AMAZONEC2_REGION provided");
            }

            if(this.config.DOCKER_MACHINE_DRIVER_AMAZONEC2_ACCESS_KEY != null) {
                String commandPart = "--amazonec2-access-key '"+this.config.DOCKER_MACHINE_DRIVER_AMAZONEC2_ACCESS_KEY+"' ";
                command.append(commandPart);
            } else {
                throw new InvalidProvisionParams("Invalid DOCKER_MACHINE_DRIVER_AMAZONEC2_ACCESS_KEY provided");
            }

            if(this.config.DOCKER_MACHINE_DRIVER_AMAZONEC2_SECRET_KEY != null) {
                String commandPart = "--amazonec2-secret-key '"+this.config.DOCKER_MACHINE_DRIVER_AMAZONEC2_SECRET_KEY+"' ";
                command.append(commandPart);
            } else {
                throw new InvalidProvisionParams("Invalid DOCKER_MACHINE_DRIVER_AMAZONEC2_SECRET_KEY provided");
            }

            if(this.config.DOCKER_MACHINE_DRIVER_AMAZONEC2_VPC_ID != null) {
                String commandPart = "--amazonec2-vpc-id '"+this.config.DOCKER_MACHINE_DRIVER_AMAZONEC2_VPC_ID+"' ";
                command.append(commandPart);
            } else {
                throw new InvalidProvisionParams("Invalid DOCKER_MACHINE_DRIVER_AMAZONEC2_VPC_ID provided");
            }

            if(this.config.DOCKER_MACHINE_DRIVER_AMAZONEC2_ZONE != null) {
                String commandPart = "--amazonec2-zone '"+this.config.DOCKER_MACHINE_DRIVER_AMAZONEC2_ZONE+"' ";
                command.append(commandPart);
            } else {
                throw new InvalidProvisionParams("Invalid DOCKER_MACHINE_DRIVER_AMAZONEC2_ZONE provided");
            }

            if(this.config.DOCKER_MACHINE_DRIVER_AMAZONEC2_INSTANCE_TYPE != null) {
                String commandPart = "--amazonec2-instance-type '"+this.config.DOCKER_MACHINE_DRIVER_AMAZONEC2_INSTANCE_TYPE+"' ";
                command.append(commandPart);
            } else {
                throw new InvalidProvisionParams("Invalid DOCKER_MACHINE_DRIVER_AMAZONEC2_INSTANCE_TYPE provided");
            }

            if(this.config.DOCKER_MACHINE_DRIVER_AMAZONEC2_SECURITY_GROUP != null) {
                String commandPart = "--amazonec2-security-group '"+this.config.DOCKER_MACHINE_DRIVER_AMAZONEC2_SECURITY_GROUP+"' ";
                command.append(commandPart);
            } else {
                throw new InvalidProvisionParams("Invalid DOCKER_MACHINE_DRIVER_AMAZONEC2_SECURITY_GROUP provided");
            }

            if(this.config.DOCKER_MACHINE_DRIVER_AMAZONEC2_SSH_USER != null) {
                String commandPart = "--amazonec2-ssh-user '"+this.config.DOCKER_MACHINE_DRIVER_AMAZONEC2_SSH_USER+"' ";
                command.append(commandPart);
            }

            if(this.config.DOCKER_MACHINE_DRIVER_AMAZONEC2_AMI != null) {
                String commandPart = "--amazonec2-ami '"+this.config.DOCKER_MACHINE_DRIVER_AMAZONEC2_AMI+"' ";
                command.append(commandPart);
            }

            if(this.config.DOCKER_MACHINE_DRIVER_AMAZONEC2_SUBNET_ID != null) {
                String commandPart = "--amazonec2-subnet-id '"+this.config.DOCKER_MACHINE_DRIVER_AMAZONEC2_SUBNET_ID+"' ";
                command.append(commandPart);
            }

            if(this.config.DOCKER_MACHINE_DRIVER_AMAZONEC2_PRIVATE_ADDRESS_ONLY) {
                String commandPart = "--amazonec2-private-address-only ";
                command.append(commandPart);
            }

            command.append(workerName);

            Debugger.getInstance().debug("Machine provision command: " + command, this.getClass());
            String creationOutput = CommandExecutor.getInstance().execute(command.toString());
            Debugger.getInstance().debug("Machine provisioned! - "+creationOutput, this.getClass());

            if(Stats.getInstance().isEnabled()) {
                Stats.getInstance().get().increment("minicortex.observers.workers.provision");
            }

        } catch (IOException e) {
            System.out.println(Debugger.PREPEND_OUTPUT_DOCKER + "EXCEPTION: " + e.getMessage());
        }

    }

    /**
     * Get stopped workers
     * @return ArrayList
     */
    public ArrayList<Worker> getStoppedWorkers() {
        return this.getWorkers(Worker.STATUS_STOPPED);
    }

    /**
     * Get ERROR workers
     * @return ArrayList
     */
    public ArrayList<Worker> getErrorWorkers() {
        return this.getWorkers(Worker.STATUS_ERROR);
    }

    /**
     * Get running workers
     * @return ArrayList
     */
    public ArrayList<Worker> getRunningWorkers() {
        return this.getWorkers(Worker.STATUS_RUNNING);
    }

    /**
     * Get stopping workers
     * @return ArrayList
     */
    public ArrayList<Worker> getStoppingWorkers() {
        return this.getWorkers(Worker.STATUS_STOPPING);
    }

    /**
     * Get starting workers
     * @return ArrayList
     */
    public ArrayList<Worker> getStartingWorkers() {
        return this.getWorkers(Worker.STATUS_STARTING);
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
        Worker workerToStart = this.getRandomWorker(Worker.STATUS_STOPPED);
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
        if(state.equals(Worker.STATUS_STOPPED)) {
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