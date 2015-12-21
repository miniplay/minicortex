package com.miniplay.minicortex.exceptions;

/**
 * Created by ret on 11/12/15.
 */
public class DependenciesNotInstalled extends Exception {

    public DependenciesNotInstalled() { super(); }
    public DependenciesNotInstalled(String message) { super(message); }
    public DependenciesNotInstalled(String message, Throwable cause) { super(message, cause); }
    public DependenciesNotInstalled(Throwable cause) { super(cause); }

}
