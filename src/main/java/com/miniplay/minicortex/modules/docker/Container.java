package com.miniplay.minicortex.modules.docker;

import com.miniplay.common.CommandExecutor;
import com.miniplay.common.Debugger;
import com.miniplay.minicortex.config.ConfigManager;
import com.miniplay.minicortex.modules.balancer.ElasticBalancer;

import java.io.IOException;

/**
 *
 * Created by ret on 7/12/15.
 */
public class Container {

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


    protected String name = null;
    protected String driver = null;
    protected String state = null;
    protected String url = null;


    /**
     * Container Constructor
     * @param name String
     * @param driver String
     * @param state String
     * @param url String
     */
    public Container(String name, String driver, String state, String url) {
        this.name = name;
        this.driver = driver;
        this.state = state;
        this.url = url;
    }


    /**
     * Container actions------------------------------------------------------------------------------------------------
     */


    /**
     * Start container
     */
    public void start() {
        Debugger.getInstance().print("Container #" + this.getName() + " is going to start...",this.getClass());
        this.changeState(ACTION_START);
        if(ElasticBalancer.getInstance().getStatsd() != null) {
            ElasticBalancer.getInstance().getStatsd().increment("minicortex.elastic_balancer.containers.container.start");
        }
    }

    /**
     * Kill container (stop)
     */
    public void kill() {
        String killMode = ElasticBalancer.getInstance().getContainerManager().getConfig().DOCKER_KILL_MODE;
        Debugger.getInstance().print("Container #" + this.getName() + " is being ["+killMode+"] killed",this.getClass());
        if(killMode.toUpperCase().equals("SOFT")) {
            this.softKill();
            if(ElasticBalancer.getInstance().getStatsd() != null) {
                ElasticBalancer.getInstance().getStatsd().increment("minicortex.elastic_balancer.containers.container.softkill");
            }
        } else {
            // Hard kill the machine, the worker would need to handle the shutdown sigterm
            this.changeState(ACTION_KILL);
            if(ElasticBalancer.getInstance().getStatsd() != null) {
                ElasticBalancer.getInstance().getStatsd().increment("minicortex.elastic_balancer.containers.container.hardkill");
            }
        }
    }

    /**
     * Soft KILL a container (this touche's a file into the container and the worker would autokill itself when he can.
     */
    private void softKill() {
        String directoryPath = ElasticBalancer.getInstance().getContainerManager().getConfig().DOCKER_SOFT_KILL_PATH;
        String dieFileName = ElasticBalancer.getInstance().getContainerManager().getConfig().DOCKER_SOFT_KILL_FILENAME;

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
     * Remove container (also terminates the instance in AWS)
     */
    public void remove() {
        this.changeState(ACTION_RM);
    }

    /**
     * Change the container state (start|kill|rm...)
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
