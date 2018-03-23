package com.rodgermajor.androidgeek.bluetoothchat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.rodgermajor.androidgeek.common.logger.mLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class MyChatService {
    // Debugging
    private static final String TAG = "myChatService";

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("ba9ccfa6-65af-4f35-8ee1-1453c8869a93");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mySecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread myConnectThread;
    private ConnectedThread myConnectedThread;
    private int mState;
    private int mNewState;

    // MyConstants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /**
     * Constructor. Prepares a new BluetoothChat session.
     */
    public MyChatService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mNewState = mState;
        mHandler = handler;
    }

    /**
     * Update UI title according to the current state of the chat connection
     */
    private synchronized void updateUI_Title() {
        mState = getState();
        mLog.d(TAG, "updateUI_Title() " + mNewState + " -> " + mState);
        mNewState = mState;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(MyConstants.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        mLog.d(TAG, "start");

        // Cancel any thread attempting to make a connection

        if (myConnectThread != null) {
            myConnectThread.cancel();
            myConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (myConnectedThread != null) {
            myConnectedThread.cancel();
            myConnectedThread = null;
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (mySecureAcceptThread == null) {
            mySecureAcceptThread = new AcceptThread(true);
            mySecureAcceptThread.start();
        }
//        if (mInsecureAcceptThread == null) {
//            mInsecureAcceptThread = new AcceptThread(false);
//            mInsecureAcceptThread.start();
//        }
        // Update UI title
        updateUI_Title();
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     */
    public synchronized void coonnect(BluetoothDevice device, boolean secure) {
        mLog.d(TAG, "coonnect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (myConnectThread != null) {
                myConnectThread.cancel();
                myConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (myConnectedThread != null) {
            myConnectedThread.cancel();
            myConnectedThread = null;
        }

        // Start the thread to coonnect with the given device
        myConnectThread = new ConnectThread(device, secure);
        myConnectThread.start();
        // Update UI title
        updateUI_Title();
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        mLog.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        if (myConnectThread != null) {
            myConnectThread.cancel();
            myConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (myConnectedThread != null) {
            myConnectedThread.cancel();
            myConnectedThread = null;
        }

        // Cancel the accept thread because we only want to coonnect to one device
        if (mySecureAcceptThread != null) {
            mySecureAcceptThread.cancel();
            mySecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        myConnectedThread = new ConnectedThread(socket, socketType);
        myConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MyConstants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MyConstants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        // Update UI title
        updateUI_Title();
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        mLog.d(TAG, "stop");

        if (myConnectThread != null) {
            myConnectThread.cancel();
            myConnectThread = null;
        }

        if (myConnectedThread != null) {
            myConnectedThread.cancel();
            myConnectedThread = null;
        }

        if (mySecureAcceptThread != null) {
            mySecureAcceptThread.cancel();
            mySecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        mState = STATE_NONE;
        // Update UI title
        updateUI_Title();
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = myConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MyConstants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MyConstants.TOAST, "Unable to coonnect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        // Update UI title
        updateUI_Title();

        // Start the service over to restart listening mode
        MyChatService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MyConstants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MyConstants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        // Update UI title
        updateUI_Title();

        // Start the service over to restart listening mode
        MyChatService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MY_UUID_SECURE);
                }else {
                    // do nothing
                }
            } catch (IOException e) {
                mLog.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
            mState = STATE_LISTEN;
        }

        public void run() {
            mLog.d(TAG, "Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    mLog.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (MyChatService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(),
                                        mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    mLog.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            mLog.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            mLog.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                mLog.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mySocket;
        private final BluetoothDevice myDevice;
        private String mySocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            myDevice = device;
            BluetoothSocket tmp = null;
            mySocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
                } else {
                   tmp = device.createRfcommSocketToServiceRecord(
                           MY_UUID_SECURE
                   );
                }
            } catch (IOException e) {
                mLog.e(TAG, "Socket Type: " + mySocketType + "create() failed", e);
            }
            mySocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            mLog.i(TAG, "BEGIN myConnectThread SocketType:" + mySocketType);
            setName("ConnectThread" + mySocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mySocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mySocket.close();
                } catch (IOException e2) {
                    mLog.e(TAG, "unable to close() " + mySocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (MyChatService.this) {
                myConnectThread = null;
            }

            // Start the connected thread
            connected(mySocket, myDevice, mySocketType);
        }

        public void cancel() {
            try {
                mySocket.close();
            } catch (IOException e) {
                mLog.e(TAG, "close() of coonnect " + mySocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mySocket;
        private final InputStream myInStream;
        private final OutputStream myOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            mLog.d(TAG, "create ConnectedThread: " + socketType);
            mySocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                mLog.e(TAG, "temp sockets not created", e);
            }

            myInStream = tmpIn;
            myOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        public void run() {
            mLog.i(TAG, "BEGIN myConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = myInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(MyConstants.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    mLog.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         */
        public void write(byte[] buffer) {
            try {
                myOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(MyConstants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                mLog.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mySocket.close();
            } catch (IOException e) {
                mLog.e(TAG, "close() of coonnect socket failed", e);
            }
        }
    }
}
