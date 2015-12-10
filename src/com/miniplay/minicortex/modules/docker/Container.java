package com.miniplay.minicortex.modules.docker;

import com.miniplay.common.CommandExecutor;

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
        this.changeState(this.ACTION_START);
    }

    /**
     * Kill container (stop)
     */
    public void kill() {
        this.changeState(this.ACTION_KILL);
    }

    /**
     * Remove container (also terminates the instance in AWS)
     */
    public void remove() {
        this.changeState(this.ACTION_RM);
    }

    /**
     * Change the container state (start|kill|rm...)
     * @param action String
     */
    private void changeState(String action) {
        String commandOutput = CommandExecutor.getInstance().execute("docker-machine " + action  + " " + this.getName());
        System.out.println(commandOutput);
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
