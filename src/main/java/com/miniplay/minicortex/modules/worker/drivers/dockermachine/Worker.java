package com.miniplay.minicortex.modules.worker.drivers.dockermachine;

import com.miniplay.common.CommandExecutor;
import com.miniplay.common.Debugger;
import com.miniplay.common.Stats;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;
import com.miniplay.minicortex.modules.worker.drivers.AbstractWorker;

import java.io.IOException;

public class Worker extends AbstractWorker {

    public static final String STATUS_RUNNING = "Running";
    public static final String STATUS_PAUSED = "Paused";
    public static final String STATUS_SAVED = "Saved";
    public static final String STATUS_STOPPED = "Stopped";
    public static final String STATUS_STOPPING = "Stopping";
    public static final String STATUS_STARTING = "Starting";
    public static final String STATUS_ERROR = "Error";


    private static final String ACTION_START = "start";
    private static final String ACTION_KILL = "kill";
    private static final String ACTION_RM = "rm";
    private static final String ACTION_CREATE = "create";
    private final DockerMachineDriver driverInstance;


    protected String name = null;
    protected String driver = null;
    protected String state = null;
    protected String url = null;


    /**
     * Worker Constructor
     * @param name String
     * @param driver String
     * @param state String
     * @param url String
     */
    public Worker(DockerMachineDriver driverInstance, String name, String driver, String state, String url) {
        super(name,state);
        this.driverInstance = driverInstance;
        this.name = name;
        this.driver = driver;
        this.state = state;
        this.url = url;
    }


    /**
     * Worker actions------------------------------------------------------------------------------------------------
     */


    /**
     * Start worker
     */
    public void start() {
        Debugger.getInstance().print("Worker #" + this.getName() + " is going to start...",this.getClass());
        this.changeState(ACTION_START);
        if(Stats.getInstance().isEnabled()) {
            Stats.getInstance().get().increment("minicortex.elastic_balancer.workers.worker.start");
        }
        // Add worker to scheduled stop
        driverInstance.workersScheduledStart.put(this.getName(),this);
    }

    /**
     * Kill worker (stop)
     */
    public void kill() {
        String killMode = ElasticBalancer.getInstance().getWorkerManager().getWorkerDriver().getConfig().DOCKER_MACHINE_KILL_MODE;
        Debugger.getInstance().print("Worker #" + this.getName() + " is being ["+killMode+"] killed",this.getClass());
        if(killMode.toUpperCase().equals("SOFT")) {
            this.softKill();
            if(Stats.getInstance().isEnabled()) {
                Stats.getInstance().get().increment("minicortex.elastic_balancer.workers.worker.softkill");
            }
        } else {
            // Hard kill the machine, the worker would need to handle the shutdown sigterm
            this.changeState(ACTION_KILL);
            if(Stats.getInstance().isEnabled()) {
                Stats.getInstance().get().increment("minicortex.elastic_balancer.workers.worker.hardkill");
            }
        }
        // Add worker to scheduled stop
        driverInstance.workersScheduledStop.put(this.getName(),this);
    }

    /**
     * Soft KILL a worker (this touche's a file into the worker and the worker would autokill itself when he can.
     */
    private void softKill() {
        String directoryPath = ElasticBalancer.getInstance().getWorkerManager().getWorkerDriver().getConfig().DOCKER_MACHINE_SOFT_KILL_PATH;
        String dieFileName = ElasticBalancer.getInstance().getWorkerManager().getWorkerDriver().getConfig().DOCKER_MACHINE_SOFT_KILL_FILENAME;

        // Check if directory includes / at the end, if not append it
        String lastCharacterOfPath = directoryPath.substring(directoryPath.length() - 1);
        if(!lastCharacterOfPath.equals("/")) {
            directoryPath += "/";
        }

        try {
            // Create directory if it doesn't exist
            CommandExecutor.getInstance().execute("docker-machine ssh " + this.getName() + " \"mkdir -p " + directoryPath + "; chmod 777 " +directoryPath+ "\"");

            // Touch file into directory
            CommandExecutor.getInstance().execute("docker-machine ssh " + this.getName() + " \" touch " + directoryPath + dieFileName + "; chmod 777 " + directoryPath + dieFileName +"\"");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Remove worker (also terminates the instance in AWS)
     */
    public void remove() {
        this.changeState(ACTION_RM);
    }

    /**
     * Change the worker state (start|kill|rm...)
     * @param action String
     */
    private void changeState(String action) {
        try {
            String commandOutput = CommandExecutor.getInstance().execute("docker-machine " + action  + " " + this.getName());
            System.out.println(commandOutput);
        } catch (IOException e) {
            System.out.println(Debugger.PREPEND_OUTPUT_DOCKER + "CONTAINER EXCEPTION: " + e.getMessage());
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
    public String getDriver() {
        return driver;
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
    public String getUrl() {
        return url;
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
     * @param url String
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @param driver String
     */
    public void setDriver(String driver) {
        this.driver = driver;
    }

    /**
     * @param name String
     */
    public void setName(String name) {
        this.name = name;
    }

}
