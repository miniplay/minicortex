package com.miniplay.minicortex.modules.docker;

/**
 *
 * Created by ret on 7/12/15.
 */
public class ContainerItem {

    protected String name = null;
    protected Boolean isActive = false;
    protected String driver = null;
    protected String state = null;
    protected String url = null;


    /**
     * ContainerItem Constructor
     * @param name
     * @param isActive
     * @param driver
     * @param state
     * @param url
     */
    public ContainerItem(String name, Boolean isActive, String driver, String state, String url) {
        this.name = name;
        this.isActive = isActive;
        this.driver = driver;
        this.state = state;
        this.url = url;
    }

    /**
     * Empty constructor
     */
    public ContainerItem() {

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
     * @return Boolean
     */
    public Boolean getActive() {
        return isActive;
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
     * @param active Boolean
     */
    public void setActive(Boolean active) {
        isActive = active;
    }

    /**
     * @param name String
     */
    public void setName(String name) {
        this.name = name;
    }

}
