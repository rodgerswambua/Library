package com.rodgermajor.androidgeek.common.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.rodgermajor.androidgeek.common.logger.mLog;
import com.rodgermajor.androidgeek.common.logger.mMLogWrapper;

/**
 * Base launcher activity, to handle most of the common plumbing for samples.
 */
public class BaseLauncher extends FragmentActivity {

    public static final String TAG = "BaseLauncher";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected  void onStart() {
        super.onStart();
        initializeLogging();
    }

    /** Set up targets to receive log data */
    public void initializeLogging() {
        mMLogWrapper mLogWrapper = new mMLogWrapper();
        mLog.setLogNode(mLogWrapper);

        mLog.i(TAG, "Ready");
    }
}
