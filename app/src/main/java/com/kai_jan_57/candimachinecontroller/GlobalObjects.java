package com.kai_jan_57.candimachinecontroller;

import android.bluetooth.BluetoothSocket;

import java.io.InputStream;
import java.io.OutputStream;

public class GlobalObjects {
    public static BluetoothSocket mmSocket;
    public static String connect_device;
    public static InputStream nxtOUT;
    public static OutputStream nxtIN;
}
