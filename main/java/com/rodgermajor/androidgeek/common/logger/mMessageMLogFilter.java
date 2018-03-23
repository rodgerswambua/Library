package com.rodgermajor.androidgeek.common.logger;

/**
 * Simple filter, removes everything except the message.
 * Useful for situations like on-screen log output where you don't want a lot of metadata displayed,
 * just easy-to-read message updates as they're happening.
 */
public class mMessageMLogFilter implements mLogNode {

    mLogNode mNext;

    /**
     * Takes the "next" mLogNode as a parameter, to simplify chaining.
     */
    public mMessageMLogFilter(mLogNode next) {
        mNext = next;
    }

    public mMessageMLogFilter() {
    }

    @Override
    public void println(int priority, String tag, String msg, Throwable tr) {
        if (mNext != null) {
            getNext().println(mLog.NONE, null, msg, null);
        }
    }

    /**
     * Returns the next mLogNode in the chain.
     */
    public mLogNode getNext() {
        return mNext;
    }

    /**
     * Sets the mLogNode data will be sent to..
     */
    public void setNext(mLogNode node) {
        mNext = node;
    }

}
