package com.rodgermajor.androidgeek.common.logger;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

/**
 * Simple fraggment which contains a LogView and uses is to output log data it receives
 * through the mLogNode interface.
 */
public class mLogFragment extends Fragment {

    private mMLogView mMLogView;
    private ScrollView mScrollView;

    public mLogFragment() {}

    public View inflateViews() {
        mScrollView = new ScrollView(getActivity());
        ViewGroup.LayoutParams scrollParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mScrollView.setLayoutParams(scrollParams);

        mMLogView = new mMLogView(getActivity());
        ViewGroup.LayoutParams logParams = new ViewGroup.LayoutParams(scrollParams);
        logParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        mMLogView.setLayoutParams(logParams);
        mMLogView.setClickable(true);
        mMLogView.setFocusable(true);
        mMLogView.setTypeface(Typeface.MONOSPACE);

        // Want to set padding as 16 dips, setPadding takes pixels.
        int paddingDips = 16;
        double scale = getResources().getDisplayMetrics().density;
        int paddingPixels = (int) ((paddingDips * (scale)) + .5);
        mMLogView.setPadding(paddingPixels, paddingPixels, paddingPixels, paddingPixels);
        mMLogView.setCompoundDrawablePadding(paddingPixels);

        mMLogView.setGravity(Gravity.BOTTOM);
        mMLogView.setTextAppearance(getActivity(), android.R.style.TextAppearance_Holo_Medium);

        mScrollView.addView(mMLogView);
        return mScrollView;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View result = inflateViews();

        mMLogView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
        return result;
    }

    public mMLogView getLogView() {
        return mMLogView;
    }
}