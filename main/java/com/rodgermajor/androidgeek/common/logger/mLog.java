package com.rodgermajor.androidgeek.common.logger;

/**
 * Helper class for a list (or tree) of LoggerNodes.
 *
 * Most of the methods in this class server only to map a method call in mLog to its equivalent
 * in mLogNode.
 */
public class mLog {
    // Grabbing the native values from Android's native logging facilities,
    // to make for easy migration and interop.
    public static final int NONE = -1;
    public static final int VERBOSE = android.util.Log.VERBOSE;
    public static final int DEBUG = android.util.Log.DEBUG;
    public static final int INFO = android.util.Log.INFO;
    public static final int WARN = android.util.Log.WARN;
    public static final int ERROR = android.util.Log.ERROR;
    public static final int ASSERT = android.util.Log.ASSERT;

    // Stores the beginning of the mLogNode topology.
    private static com.rodgermajor.androidgeek.common.logger.mLogNode mMLogNode;

    /**
     * Returns the next mLogNode in the linked list.
     */
    public static com.rodgermajor.androidgeek.common.logger.mLogNode getLogNode() {
        return mMLogNode;
    }

    /**
     * Sets the mLogNode data will be sent to.
     */
    public static void setLogNode(com.rodgermajor.androidgeek.common.logger.mLogNode node) {
        mMLogNode = node;
    }

    public static void println(int priority, String tag, String msg, Throwable tr) {
        if (mMLogNode != null) {
            mMLogNode.println(priority, tag, msg, tr);
        }
    }

    /**
     * Instructs the mLogNode to print the log data provided.
     */
    public static void println(int priority, String tag, String msg) {
        println(priority, tag, msg, null);
    }

   /**
     * Prints a message at VERBOSE priority.
     */
    public static void v(String tag, String msg, Throwable tr) {
        println(VERBOSE, tag, msg, tr);
    }

    public static void v(String tag, String msg) {
        v(tag, msg, null);
    }


    /**
     * Prints a message at DEBUG priority.
     */
    public static void d(String tag, String msg, Throwable tr) {
        println(DEBUG, tag, msg, tr);
    }

    public static void d(String tag, String msg) {
        d(tag, msg, null);
    }

    /**
     * Prints a message at INFO priority.
     */
    public static void i(String tag, String msg, Throwable tr) {
        println(INFO, tag, msg, tr);
    }

    public static void i(String tag, String msg) {
        i(tag, msg, null);
    }

    /**
     * Prints a message at WARN priority.
     */
    public static void w(String tag, String msg, Throwable tr) {
        println(WARN, tag, msg, tr);
    }

    /**
     * Prints a message at WARN priority.
     */
    public static void w(String tag, String msg) {
        w(tag, msg, null);
    }

    /**
     * Prints a message at WARN priority.
     */
    public static void w(String tag, Throwable tr) {
        w(tag, null, tr);
    }

    /**
     * Prints a message at ERROR priority.
     */
    public static void e(String tag, String msg, Throwable tr) {
        println(ERROR, tag, msg, tr);
    }

    public static void e(String tag, String msg) {
        e(tag, msg, null);
    }

    /**
     * Prints a message at ASSERT priority.
     */
    public static void wtf(String tag, String msg, Throwable tr) {
        println(ASSERT, tag, msg, tr);
    }

    /**
     * Prints a message at ASSERT priority.
     *
     * @param tag Tag for for the log data. Can be used to organize log statements.
     * @param msg The actual message to be logged.
     */
    public static void wtf(String tag, String msg) {
        wtf(tag, msg, null);
    }

    /**
     * Prints a message at ASSERT priority.
     *
     * @param tag Tag for for the log data. Can be used to organize log statements.
     * @param tr If an exception was thrown, this can be sent along for the logging facilities
     *           to extract and print useful information.
     */
    public static void wtf(String tag, Throwable tr) {
        wtf(tag, null, tr);
    }
}
