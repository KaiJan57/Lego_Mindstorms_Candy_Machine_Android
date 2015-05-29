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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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


public class ActivityConnect extends AppCompatActivity {

    public BluetoothSocket mmSocket;
    public BluetoothDevice mmDevice;
    public InputStream nxtOUT;
    public OutputStream nxtIN;
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    Spinner spinner2;
    static BluetoothAdapter btAdapter;
    ArrayList<String> btdevices = new ArrayList<String>();
    ArrayList<BluetoothDevice> btdev = new ArrayList<BluetoothDevice>();

    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        spinner2 = (Spinner) findViewById(R.id.spinner1);
        savedInstanceState.putInt("selected_item", (int) spinner2.getSelectedItemPosition());
        Toast.makeText(this, String.valueOf(savedInstanceState.getInt("selected_item")), Toast.LENGTH_SHORT);
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        spinner2 = (Spinner) findViewById(R.id.spinner1);
        spinner2.setSelection(savedInstanceState.getInt("selected_item"));
        Toast.makeText(this, String.valueOf(savedInstanceState.getInt("selected_item")), Toast.LENGTH_SHORT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

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
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        btupdate();
    }

    public void btupdate() {
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        for (BluetoothDevice bt : pairedDevices) {
            btdevices.add(bt.getName());
            btdev.add(bt);
        }

        //Toast.makeText(getApplicationContext(), String.valueOf(btdevices.size()), Toast.LENGTH_SHORT).show();
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

    public final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
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
                    case BluetoothAdapter.STATE_TURNING_OFF:

                        break;
                    case BluetoothAdapter.STATE_ON:
                        btupdate();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        btdev.clear();
                        btdevices.clear();
                        break;
                }
            }
        }
    };
}
