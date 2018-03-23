package com.rodgermajor.androidgeek.common.logger;

/**
 * Basic interface for a logging system that can output to one or more targets.
 */
public interface mLogNode {

    /**
     * Instructs first mLogNode in the list to print the log data provided.
     */
    public void println(int priority, String tag, String msg, Throwable tr);

}
