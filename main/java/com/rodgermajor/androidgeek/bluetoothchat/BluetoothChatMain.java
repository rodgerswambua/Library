package com.rodgermajor.androidgeek.bluetoothchat;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.rodgermajor.androidgeek.common.logger.mLog;

import static android.widget.Toast.makeText;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothChatMain extends Fragment {

    private static final String TAG = "BluetoothChatMain";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private ListView myConversationView;
    private EditText myOutEditText;
    private Button mySendButton;

    /**
     * Name of the connected device
     */
    private String myConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> myConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer myOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter myBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private MyChatService MyChatService = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // Get local Bluetooth adapter
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (myBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!myBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (MyChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (MyChatService != null) {
            MyChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (MyChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (MyChatService.getState() == MyChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                MyChatService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        myConversationView = (ListView) view.findViewById(R.id.in);
        myOutEditText = (EditText) view.findViewById(R.id.edit_text_out);
        mySendButton = (Button) view.findViewById(R.id.button_send);
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        mLog.d(TAG, "setupChat()");
        // Initialize the array adapter for the conversation thread
        myConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);

        myConversationView.setAdapter(myConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        myOutEditText.setOnEditorActionListener(myWriteListener);

        // Initialize the send button with a listener that for click events
        mySendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                if (null != view) {
                    TextView textView = (TextView) view.findViewById(R.id.edit_text_out);
                    String message = textView.getText().toString();
                    sendMessage(message);
                }
            }
        });

        // Initialize the ChatService to perform bluetooth connections
        MyChatService = new MyChatService(getActivity(), myHandler);

        // Initialize the buffer for outgoing messages
        myOutStringBuffer = new StringBuffer("");
    }

    /**
     * Makes this device discoverable for 360 seconds (6 minutes).
     */
    private void ensureDiscoverable() {
        if (myBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 360);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (MyChatService.getState() != MyChatService.STATE_CONNECTED) {
            makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the ChatService to write
            byte[] send = message.getBytes();
            MyChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            myOutStringBuffer.setLength(0);
            myOutEditText.setText(myOutStringBuffer);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener myWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    /**
     * Updates the status on the action bar.
     * Either the bluetooth is connected or not.
     */
    private void setTheStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     */
    private void setTheStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the ChatService
     */
    @SuppressLint("HandlerLeak")
    private final Handler myHandler;

    {
        myHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                FragmentActivity activity = getActivity();
                switch (msg.what) {
                    case MyConstants.MESSAGE_STATE_CHANGE:
                        switch (msg.arg1) {
                            case com.rodgermajor.androidgeek.bluetoothchat.MyChatService.STATE_CONNECTED:
                                setTheStatus(getString(R.string.title_connected_to, myConnectedDeviceName));
                                myConversationArrayAdapter.clear();
                                break;
                            case com.rodgermajor.androidgeek.bluetoothchat.MyChatService.STATE_CONNECTING:
                                setTheStatus(R.string.title_connecting);
                                break;
                            case com.rodgermajor.androidgeek.bluetoothchat.MyChatService.STATE_LISTEN:
                            case com.rodgermajor.androidgeek.bluetoothchat.MyChatService.STATE_NONE:
                                setTheStatus(R.string.title_not_connected);
                                break;
                        }
                        break;
                    case MyConstants.MESSAGE_WRITE:
                        byte[] writeBuf = (byte[]) msg.obj;
                        // construct a string from the buffer
                        String writeMessage = new String(writeBuf);
                        myConversationArrayAdapter.add("Me:  " + writeMessage);
                        break;
                    case MyConstants.MESSAGE_READ:
                        byte[] readBuf = (byte[]) msg.obj;
                        // construct a string from the valid bytes in the buffer
                        String readMessage = new String(readBuf, 0, msg.arg1);
                        myConversationArrayAdapter.add(myConnectedDeviceName + ":  " + readMessage);
                        break;
                    case MyConstants.MESSAGE_DEVICE_NAME:
                        // save the connected device's name
                        myConnectedDeviceName = msg.getData().getString(MyConstants.DEVICE_NAME);
                        if (null != activity) {
                            makeText(activity, "Connected to "
                                    + myConnectedDeviceName, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case MyConstants.MESSAGE_TOAST:
                        if (null != activity) {
                            makeText(activity, msg.getData().getString(MyConstants.TOAST),
                                    Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            }
        };
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceList returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    mLog.d(TAG, "BT not enabled");
                    makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other device
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceList.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = myBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        MyChatService.coonnect(device, secure);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceList to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceList.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
            case R.id.about: {
                Intent intent = new Intent(getContext().getApplicationContext(),About.class);
                startActivity(intent);
            }

            case R.id.share: {
                Intent sendItent = new Intent();
                sendItent.setAction(Intent.ACTION_SEND);
                sendItent.putExtra(Intent.EXTRA_TEXT, "Hi do you know that you can chat with friends and family with no data or airtime charges?Try BluetoothChat android application for free" +
                        "Download it too from here. https://www.rmajor.co.ke");
                sendItent.setType("text/plain");
                startActivity(Intent.createChooser(sendItent, "Share with..."));
            }
        }
        return false;
    }

}
