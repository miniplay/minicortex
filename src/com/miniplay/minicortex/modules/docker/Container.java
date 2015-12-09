package com.miniplay.minicortex.modules.docker;

/**
 *
 * Created by ret on 7/12/15.
 */
public class Container {

    protected String name = null;
    protected String driver = null;
    protected String state = null;
    protected String url = null;


    /**
     * Container Constructor
     * @param name
     * @param driver
     * @param state
     * @param url
     */
    public Container(String name, String driver, String state, String url) {
        this.name = name;
        this.driver = driver;
        this.state = state;
        this.url = url;
    }

    /**
     * Empty constructor
     */
    public Container() {

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
