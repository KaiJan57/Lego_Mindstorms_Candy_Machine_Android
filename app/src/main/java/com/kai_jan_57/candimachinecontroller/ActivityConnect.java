package com.kai_jan_57.candimachinecontroller;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.*;


public class ActivityConnect extends AppCompatActivity {

    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    static BluetoothAdapter btAdapter;
    public BluetoothSocket mmSocket;
    public BluetoothDevice mmDevice;
    public InputStream nxtOUT;
    public OutputStream nxtIN;
    Spinner spinner2;
    int selection;
    ArrayList<String> btdevices;
    ArrayList<BluetoothDevice> btdev;
    public final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(EXTRA_STATE, ERROR);
                switch (state) {
                    case STATE_OFF:
                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case DialogInterface.BUTTON_POSITIVE:
                                        btAdapter.enable();
                                        break;
                                    case DialogInterface.BUTTON_NEGATIVE:
                                        unregisterReceiver(mReceiver);
                                        android.os.Process.killProcess(android.os.Process.myPid());
                                        break;
                                }
                            }
                        };

                        AlertDialog.Builder builder = new AlertDialog.Builder(ActivityConnect.this);
                        builder.setMessage(getString(R.string.bt_activate_message))
                                .setPositiveButton(android.R.string.ok, dialogClickListener)
                                .setNegativeButton(android.R.string.cancel, dialogClickListener)
                                .setTitle(getString(R.string.bt_title))
                                .setCancelable(false)
                                .show();
                        break;
                    case STATE_TURNING_OFF:

                        break;
                    case STATE_ON:
                        btupdate();
                        break;
                    case STATE_TURNING_ON:
                        btdev.clear();
                        btdevices.clear();
                        break;
                }
            }
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("selected_item", spinner2.getSelectedItemPosition());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        selection = savedInstanceState.getInt("selected_item");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        btdevices = new ArrayList<String>();
        btdev = new ArrayList<BluetoothDevice>();
        setContentView(R.layout.activity_connect);

        btAdapter = getDefaultAdapter();

        if (btAdapter == null) {
            Toast.makeText(this, getString(R.string.bt_not_supported), Toast.LENGTH_LONG).show();
            unregisterReceiver(mReceiver);
            android.os.Process.killProcess(android.os.Process.myPid());
        }

        if (!btAdapter.isEnabled()) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            btAdapter.enable();
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            unregisterReceiver(mReceiver);
                            android.os.Process.killProcess(android.os.Process.myPid());
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.bt_activate_message))
                    .setPositiveButton(android.R.string.ok, dialogClickListener)
                    .setNegativeButton(android.R.string.cancel, dialogClickListener)
                    .setTitle(getString(R.string.bt_title))
                    .setCancelable(false)
                    .show();
        }
    }

    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        spinner2 = (Spinner) findViewById(R.id.spinner1);
        IntentFilter filter = new IntentFilter(ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        btupdate();
        if(String.valueOf(selection).equals("") && !btdev.isEmpty()) {
            selection = 0;
        }
        if(!btdev.isEmpty()) {
            spinner2.setSelection(selection, true);
        }
    }

    public void btupdate() {
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        for (BluetoothDevice bt : pairedDevices) {
            btdevices.add(bt.getName());
            btdev.add(bt);
        }

        spinner2 = (Spinner) findViewById(R.id.spinner1);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, btdevices);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner2.setAdapter(dataAdapter);
    }

    public void connectdevice(View v) {
        spinner2 = (Spinner) findViewById(R.id.spinner1);
        GlobalObjects.connect_device = String.valueOf(spinner2.getSelectedItem());

        BluetoothSocket tmp = null;
        mmDevice = btdev.get(btdevices.indexOf(spinner2.getSelectedItem()));
        try {
            tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            ErrorHandler(e.getMessage().toString());
        }
        mmSocket = tmp;
        GlobalObjects.mmSocket = mmSocket;
        final ProgressDialog pD = ProgressDialog.show(ActivityConnect.this, ActivityConnect.this.getString(R.string.connecting), ActivityConnect.this.getString(R.string.wait_info), true);
        pD.setCancelable(true);

        final Thread ct = new Thread(new Runnable() {
            @Override
            public void run() {
                ActivityConnect.btAdapter.cancelDiscovery();
                try {
                    mmSocket.connect();
                    startActivity(new Intent(ActivityConnect.this, ActivityControl.class));
                } catch (IOException connectException) {
                    try {
                        mmSocket.close();
                    } catch (IOException closeException) {
                        pD.dismiss();
                        ErrorHandler(closeException.getMessage().toString());
                    }
                    return;
                }
                pD.dismiss();
            }
        });

        pD.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    pD.dismiss();
                    ErrorHandler(closeException.getMessage().toString());
                }
            }
        });

        ct.start();


        try {
            nxtOUT = mmSocket.getInputStream();
            nxtIN = mmSocket.getOutputStream();
            GlobalObjects.nxtIN = nxtIN;
            GlobalObjects.nxtOUT = nxtOUT;
        } catch (IOException e) {
            ErrorHandler(e.getMessage().toString());
        }
    }

    public void ErrorHandler(String ErrorMessage) {
        new AlertDialog.Builder(getApplicationContext())
                .setCancelable(false)
                .setTitle(getString(R.string.error))
                .setMessage(ErrorMessage)
                .setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onDestroy();
                        ActivityConnect.this.recreate();
                    }
                }).create().show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    /* ... */

        // Unregister broadcast listeners
        unregisterReceiver(mReceiver);
    }
}
