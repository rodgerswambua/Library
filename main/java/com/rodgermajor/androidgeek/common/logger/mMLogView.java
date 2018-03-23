package com.rodgermajor.androidgeek.common.logger;

import android.app.Activity;
import android.content.Context;
import android.util.*;
import android.widget.TextView;

/** Simple TextView which is used to output log data received through the mLogNode interface.
*/
public class mMLogView extends TextView implements mLogNode {

    public mMLogView(Context context) {
        super(context);
    }

    public mMLogView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public mMLogView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Formats the log data and prints it out to the LogView.
     */
    @Override
    public void println(int priority, String tag, String msg, Throwable tr) {

        
        String priorityStr = null;
        switch(priority) {
            case android.util.Log.VERBOSE:
                priorityStr = "VERBOSE";
                break;
            case android.util.Log.DEBUG:
                priorityStr = "DEBUG";
                break;
            case android.util.Log.INFO:
                priorityStr = "INFO";
                break;
            case android.util.Log.WARN:
                priorityStr = "WARN";
                break;
            case android.util.Log.ERROR:
                priorityStr = "ERROR";
                break;
            case android.util.Log.ASSERT:
                priorityStr = "ASSERT";
                break;
            default:
                break;
        }

        String exceptionStr = null;
        if (tr != null) {
            exceptionStr = android.util.Log.getStackTraceString(tr);
        }

        final StringBuilder outputBuilder = new StringBuilder();

        String delimiter = "\t";
        appendIfNotNull(outputBuilder, priorityStr, delimiter);
        appendIfNotNull(outputBuilder, tag, delimiter);
        appendIfNotNull(outputBuilder, msg, delimiter);
        appendIfNotNull(outputBuilder, exceptionStr, delimiter);

        ((Activity) getContext()).runOnUiThread( (new Thread(new Runnable() {
            @Override
            public void run() {
                // Display the text we just generated within the LogView.
                appendToLog(outputBuilder.toString());
            }
        })));

        if (mNext != null) {
            mNext.println(priority, tag, msg, tr);
        }
    }

    public mLogNode getNext() {
        return mNext;
    }

    public void setNext(mLogNode node) {
        mNext = node;
    }

    /** Takes a string and adds to it, with a separator, if the bit to be added isn't null. Since
     * the logger takes so many arguments that might be null, this method helps cut out some of the
     * agonizing tedium of writing the same 3 lines over and over.
     */
    private StringBuilder appendIfNotNull(StringBuilder source, String addStr, String delimiter) {
        if (addStr != null) {
            if (addStr.length() == 0) {
                delimiter = "";
            }

            return source.append(addStr).append(delimiter);
        }
        return source;
    }
    mLogNode mNext;

    /** Outputs the string as a new line of log data in the LogView. */
    public void appendToLog(String s) {
        append("\n" + s);
    }


}
