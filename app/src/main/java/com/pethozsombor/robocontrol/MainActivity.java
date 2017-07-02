package com.pethozsombor.robocontrol;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    private static final int DEFAULT_BASE_360 = 28;
    private static final int DEFAULT_SHOULDER = 60;
    private static final int DEFAULT_ELBOW = 0;
    private static final int DEFAULT_WRIST = 50;
    private static final int DEFAULT_WRIST_360 = 3;
    private static final int DEFAULT_GRIPPER = 0;

    public static final int ACTION_MOTION = 900;


    public static final int ID_MOTION_1 = 1;
    public static final int ID_MOTION_2 = 2;
    public static final int ID_MOTION_3 = 3;
    public static final int ID_MOTION_4 = 4;
    public static final int ID_MOTION_5 = 5;

    public static final int ID_SLIDER_BASE_360 = 0;
    public static final int ID_SLIDER_SHOULDER = 1;
    public static final int ID_SLIDER_ELBOW = 2;
    public static final int ID_SLIDER_WRIST = 3;
    public static final int ID_SLIDER_WRIST_360 = 4;
    public static final int ID_SLIDER_GRIPPER = 5;


    public static final String SEPARATOR = "\n";

    private static final UUID ROBO_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final String ROBO_MAC_ADDRESS = "30:14:12:04:14:66";

    private static final int BLUETOOTH_REQUEST_CODE = 0;

    private BluetoothAdapter mBluetoothAdapter;
    private FloatingActionButton mBluetoothButton;
    private Button mStartPositionButton;
    private Button mExecuteButton;
    private Button mStopButton;
    private SeekBar mBaseSeekBar;
    private SeekBar mShoulderSeekBar;
    private SeekBar mElbowSeekBar;
    private SeekBar mWristSeekBar;
    private SeekBar mWrist360SeekBar;
    private SeekBar mGripperSeekBar;
    private RadioGroup mMotions;
    private RadioButton mMotion1;
    private RadioButton mMotion2;
    private RadioButton mMotion3;
    private RadioButton mMotion4;
    private RadioButton mMotion5;
    private boolean mIsConnected;
    private CommunicateThread mConnectedThread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Initialize the layout.
        setContentView(R.layout.activity_main);

        //Set up the action bar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Set up the Bluetooth connect / disconnect button.
        mIsConnected = false;
        mBluetoothButton = (FloatingActionButton) findViewById(R.id.connect_button);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsConnected) {
                    mConnectedThread.cancel();
                    showSnackBar(getString(R.string.connection_lost));
                    onConnectivityChange(false);
                } else {
                    if (mBluetoothAdapter == null) { //Handle no Bluetooth adapter.
                        showSnackBar(getString(R.string.no_bluetooth));
                    } else {
                        mBluetoothButton.setImageDrawable(ContextCompat.getDrawable(mBluetoothButton.getContext(), R.drawable.ic_connecting_24dp));
                        mBluetoothButton.setEnabled(false);
                        if (!mBluetoothAdapter.isEnabled()) { //Check if Bluetooth is enabled - ask for it if not.
                            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBluetoothIntent, BLUETOOTH_REQUEST_CODE);
                        } else { //Connect to Robo.
                            List<BluetoothDevice> pairedDevices = new ArrayList<>(mBluetoothAdapter.getBondedDevices());
                            if (pairedDevices.size() > 0) {
                                boolean isConnecting = false;
                                for (int i = 0; i < pairedDevices.size(); i++) {
                                    BluetoothDevice device = pairedDevices.get(i);
                                    if (device.getAddress().equals(ROBO_MAC_ADDRESS)) {
                                        (new ConnectThread(device)).start();
                                        isConnecting = true;
                                    }
                                }
                                if (!isConnecting) {
                                    onConnectivityChange(false);
                                    showSnackBar(getString(R.string.connection_error_pairing));
                                }
                            } else {
                                onConnectivityChange(false);
                                showSnackBar(getString(R.string.connection_error_pairing_none));
                            }
                        }
                    }
                }
            }
        });

        //Set up the Start Position button.
        mStartPositionButton = (Button) findViewById(R.id.button_start);
        mStartPositionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBaseSeekBar.setProgress(1);
                mBaseSeekBar.setProgress(DEFAULT_BASE_360);
                mShoulderSeekBar.setProgress(1);
                mShoulderSeekBar.setProgress(DEFAULT_SHOULDER);
                mElbowSeekBar.setProgress(1);
                mElbowSeekBar.setProgress(DEFAULT_ELBOW);
                mWristSeekBar.setProgress(1);
                mWristSeekBar.setProgress(DEFAULT_WRIST);
                mWrist360SeekBar.setProgress(1);
                mWrist360SeekBar.setProgress(DEFAULT_WRIST_360);
                mGripperSeekBar.setProgress(1);
                mGripperSeekBar.setProgress(DEFAULT_GRIPPER);
            }
        });


        int step = 1;
        int max = 95;
        int min = 45;

        //Set up the sliders.
        mBaseSeekBar = (SeekBar) findViewById(R.id.seekbar_base);
        mBaseSeekBar.setProgress(DEFAULT_BASE_360);
        mBaseSeekBar.setTag(ID_SLIDER_BASE_360);
        mBaseSeekBar.setMax((max - min) / step);
        mBaseSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int step = 1;
                int min = 45;
                mConnectedThread.write((Integer.toString((int) Math.floor(min + (progress * step))) + SEPARATOR).getBytes());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        int max1= 1020;
        int min1= 950;

        mGripperSeekBar = (SeekBar) findViewById(R.id.seekbar_gripper);
        mGripperSeekBar.setProgress(DEFAULT_GRIPPER);
        mGripperSeekBar.setTag(ID_SLIDER_GRIPPER);
        mGripperSeekBar.setMax((max1 - min1) / step);
        mGripperSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int step = 1;
                int min1 = 950;
                mConnectedThread.write((Integer.toString((int) Math.floor(min1 + (progress * step))) + SEPARATOR).getBytes());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });


        int maxW = 752;
        final int minW = 746;

        //Set up the sliders.
        mWrist360SeekBar = (SeekBar) findViewById(R.id.seekbar_wrist);
        mWrist360SeekBar.setProgress(DEFAULT_WRIST_360);
        mWrist360SeekBar.setTag(ID_SLIDER_BASE_360);
        mWrist360SeekBar.setMax((maxW - minW) / step);
        mWrist360SeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int step = 1;
                int minW = 746;
                mConnectedThread.write((Integer.toString((int) Math.floor(minW + (progress * step))) + SEPARATOR).getBytes());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mShoulderSeekBar = (SeekBar) findViewById(R.id.seekbar_shoulder);
        mShoulderSeekBar.setProgress(DEFAULT_SHOULDER);
        mShoulderSeekBar.setTag(ID_SLIDER_SHOULDER);
        mShoulderSeekBar.setOnSeekBarChangeListener(this);

        mElbowSeekBar = (SeekBar) findViewById(R.id.seekbar_elbow);
        mElbowSeekBar.setProgress(DEFAULT_ELBOW);
        mElbowSeekBar.setTag(ID_SLIDER_ELBOW);
        mElbowSeekBar.setOnSeekBarChangeListener(this);


        mWristSeekBar = (SeekBar) findViewById(R.id.seekbar_wrist_up_down);
        mWristSeekBar.setProgress(DEFAULT_WRIST);
        mWristSeekBar.setTag(ID_SLIDER_WRIST);
        mWristSeekBar.setOnSeekBarChangeListener(this);





        //Set up the motions.
        mMotions = (RadioGroup) findViewById(R.id.motions_group);
        mMotion1 = (RadioButton) findViewById(R.id.motion_1);
        mMotion2 = (RadioButton) findViewById(R.id.motion_2);
        mMotion3 = (RadioButton) findViewById(R.id.motion_3);
        mMotion4 = (RadioButton) findViewById(R.id.motion_4);
        mMotion5 = (RadioButton) findViewById(R.id.motion_5);

        mExecuteButton = (Button) findViewById(R.id.button_execute);
        mExecuteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedMotion = 0;
                switch (mMotions.getCheckedRadioButtonId()) {
                    case R.id.motion_1:
                        selectedMotion = ID_MOTION_1;
                        break;
                    case R.id.motion_2:
                        selectedMotion = ID_MOTION_2;
                        break;
                    case R.id.motion_3:
                        selectedMotion = ID_MOTION_3;
                        break;
                    case R.id.motion_4:
                        selectedMotion = ID_MOTION_4;
                        break;
                    case R.id.motion_5:
                        selectedMotion = ID_MOTION_5;
                        break;
                }
                if (mIsConnected) {
                    mConnectedThread.write((Integer.toString(ACTION_MOTION) + Integer.toString(selectedMotion) + SEPARATOR).getBytes());
                }
            }
        });

        mStopButton = (Button) findViewById(R.id.button_stop);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConnectedThread.write((Integer.toString(ACTION_MOTION) + "0" + SEPARATOR).getBytes());
            }
        });

        onConnectivityChange(false);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BLUETOOTH_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                mBluetoothButton.performClick(); //Continue connecting after the Bluetooth has been enabled.
            } else {
                onConnectivityChange(false);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                showSnackBar(getString(R.string.about_text));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        mConnectedThread.write((Integer.toString((int) Math.floor((180 * (int) seekBar.getTag()) + (progress * 1.79))) + SEPARATOR).getBytes());
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    /**
     * Enables or disables the interactive widgets.
     *
     * @param enable - Enable or disable.
     */
    private void onConnectivityChange(boolean enable) {
        mIsConnected = enable;
        mBluetoothButton.setEnabled(true);
        if (enable) {
            mBluetoothButton.setImageDrawable(ContextCompat.getDrawable(mBluetoothButton.getContext(), R.drawable.ic_bluetooth_disable_24dp));
        } else {
            mBluetoothButton.setImageDrawable(ContextCompat.getDrawable(mBluetoothButton.getContext(), R.drawable.ic_bluetooth_enable_24dp));
        }
        mBaseSeekBar.setEnabled(enable);
        mShoulderSeekBar.setEnabled(enable);
        mElbowSeekBar.setEnabled(enable);
        mWristSeekBar.setEnabled(enable);
        mWrist360SeekBar.setEnabled(enable);
        mGripperSeekBar.setEnabled(enable);
        mStartPositionButton.setEnabled(enable);
        mExecuteButton.setEnabled(enable);
        mStopButton.setEnabled(enable);
        mMotion1.setEnabled(enable);
        mMotion2.setEnabled(enable);
        mMotion3.setEnabled(enable);
        mMotion4.setEnabled(enable);
        mMotion5.setEnabled(enable);
    }

    private void showSnackBar(String text) {
        Snackbar snackBar = Snackbar.make(mBluetoothButton, text, Toast.LENGTH_SHORT);
        snackBar.getView().setBackgroundColor(ContextCompat.getColor(mBluetoothButton.getContext(), R.color.colorPrimaryDark));
        snackBar.show();
    }

    private void startCommunication(BluetoothSocket socket) {
        showSnackBar(getString(R.string.connection_successful));
        onConnectivityChange(true);
        mConnectedThread = new CommunicateThread(socket);
        mConnectedThread.start();
        mStartPositionButton.performClick();
    }

    @Override
    public void onStart() {
        super.onStart();
    }


    @Override
    public void onStop() {
        super.onStop();
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mSocket = null;


        private ConnectThread(BluetoothDevice device) {
            try {
                mSocket = device.createRfcommSocketToServiceRecord(ROBO_UUID);
            } catch (IOException ignored) {
            }
        }

        @Override
        public void run() {
            if (mSocket != null) {
                boolean success = false;
                try {
                    mSocket.connect();
                    success = true;
                } catch (IOException ignoredOne) {
                    try {
                        mSocket.close();
                    } catch (IOException ignored) {
                    }
                }

                if (success) {
                    //Connection successful.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startCommunication(mSocket);
                        }
                    });
                } else {
                    //Cannot connect.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onConnectivityChange(false);
                        }
                    });
                }
            }
        }
    }


    private class CommunicateThread extends Thread {
        private final BluetoothSocket connectedBluetoothSocket;
        private final OutputStream connectedOutputStream;

        public CommunicateThread(BluetoothSocket socket) {
            connectedBluetoothSocket = socket;
            OutputStream out = null;

            try {
                out = socket.getOutputStream();
            } catch (IOException ignored) {
            }
            connectedOutputStream = out;
        }

        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);
            } catch (IOException ignored) {
            }
        }

        public void cancel() {
            try {
                connectedBluetoothSocket.close();
            } catch (IOException ignored) {
            }
        }
    }
}