package com.rodgermajor.androidgeek.common.logger;

import android.util.Log;

/**
 * Helper class which wraps Android's native mLog utility in the Logger interface.
 */
public class mMLogWrapper implements mLogNode {

    private mLogNode mNext;
    /**
     * Returns the next mLogNode in the linked list.
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

    /**
     * Prints data out to the console using Android's native log mechanism.
     */
    @Override
    public void println(int priority, String tag, String msg, Throwable tr) {
        String useMsg = msg;
        if (useMsg == null) {
            useMsg = "";
        }

        if (tr != null) {
            msg += "\n" + Log.getStackTraceString(tr);
        }

        Log.println(priority, tag, useMsg);

        // If this isn't the last node in the chain, move things along.
        if (mNext != null) {
            mNext.println(priority, tag, msg, tr);
        }
    }
}
