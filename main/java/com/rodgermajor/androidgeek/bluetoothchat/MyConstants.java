package com.rodgermajor.androidgeek.bluetoothchat;

/**
 * Defines several constants used between {myChatService} and the UI.
 */
public interface MyConstants {

    // Message types sent from the myChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the myChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

}
