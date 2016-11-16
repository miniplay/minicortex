package com.miniplay.minicortex.modules.worker.exceptions;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Created by ret on 16/11/16.
 */
public class DriverException extends Exception {
    public DriverException() {
    }

    public DriverException(String message) {
        super(message);
    }

    public DriverException(String message, Throwable cause) {
        super(message, cause);
    }

    public DriverException(Throwable cause) {
        super(cause);
    }

    public DriverException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }

    @Override
    public String getLocalizedMessage() {
        return super.getLocalizedMessage();
    }

    @Override
    public synchronized Throwable getCause() {
        return super.getCause();
    }

    @Override
    public synchronized Throwable initCause(Throwable cause) {
        return super.initCause(cause);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public void printStackTrace() {
        super.printStackTrace();
    }

    @Override
    public void printStackTrace(PrintStream s) {
        super.printStackTrace(s);
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        super.printStackTrace(s);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return super.fillInStackTrace();
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return super.getStackTrace();
    }

    @Override
    public void setStackTrace(StackTraceElement[] stackTrace) {
        super.setStackTrace(stackTrace);
    }
}
