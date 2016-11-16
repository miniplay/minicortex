package com.miniplay.minicortex.modules.worker.drivers;


abstract public class AbstractWorker {

    protected String name = null;
    protected String state = null;

    /**
     * AbstractWorker Constructor
     * @param name String
     * @param state String
     */
    public AbstractWorker(String name, String state) {
        this.name = name;
        this.state = state;
    }


    /**
     * Worker actions------------------------------------------------------------------------------------------------
     */

    /**
     * Start worker (run)
     */
    abstract public void start();

    /**
     * Kill worker (stop)
     */
    abstract public void kill();

    /**
     * Remove worker
     */
    abstract public void remove();


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

}
