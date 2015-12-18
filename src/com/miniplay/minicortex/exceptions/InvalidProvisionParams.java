package com.miniplay.minicortex.exceptions;

/**
 * Created by ret on 11/12/15.
 */
public class InvalidProvisionParams extends Exception {

    public InvalidProvisionParams() { super(); }
    public InvalidProvisionParams(String message) { super(message); }
    public InvalidProvisionParams(String message, Throwable cause) { super(message, cause); }
    public InvalidProvisionParams(Throwable cause) { super(cause); }

}
