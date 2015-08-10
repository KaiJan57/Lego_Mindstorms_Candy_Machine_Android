package com.kai_jan_57.candimachinecontroller;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class ActivityControl extends AppCompatActivity {

    public Handler mainHandler;
    public TextView tv5;
    public TextView tvcoins;
    public String coinnumber = "0";
    public String sn1;
    public boolean readCoins = true;
    public boolean tstopped;
    public boolean using_pipeline = false;
    public CheckBox cb;
    public OutputStream nxtIN;
    public InputStream nxtOUT;
    public Thread udT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        mainHandler = new Handler(this.getMainLooper());

        nxtIN = GlobalObjects.nxtIN;
        nxtOUT = GlobalObjects.nxtOUT;
    }

    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == KeyEvent.KEYCODE_BACK) {
            final ProgressDialog pD1 = ProgressDialog.show(ActivityControl.this, ActivityControl.this.getString(R.string.disconnecting), ActivityControl.this.getString(R.string.wait_info), true);
            pD1.setCancelable(false);
            new Thread(new Runnable() {
                public void run() {
                    try {
                        readCoins = false;
                        while (!tstopped || using_pipeline) {
                            //wait
                        }
                        nxtIN.close();
                        nxtOUT.close();
                        GlobalObjects.mmSocket.close();
                    } catch (IOException e) {
                        final IOException ex = e;
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                ErrorHandler(ex.getMessage());
                            }
                        });
                    }
                    pD1.dismiss();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    });
                }
            }).start();
        }
        super.onKeyDown(i, keyEvent);
        return true;
    }

    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        tv5 = (TextView) findViewById(R.id.textView5);
        tv5.setText(GlobalObjects.connect_device);
        cb = (CheckBox) findViewById(R.id.checkBox1);
        tvcoins = (TextView) findViewById(R.id.textView3);
        startProgram(null);
        udT = new updateCoins();
        udT.start();
    }

    public void ErrorHandler(String ErrorMessage) {
        new AlertDialog.Builder(getApplicationContext())
                .setCancelable(false)
                .setTitle(getString(R.string.error))
                .setMessage(ErrorMessage)
                .setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityControl.this.recreate();
                    }
                }).create().show();
    }

    public void coinsyschanged(View view) {
        if (GlobalObjects.mmSocket.isConnected()) {
            final ProgressDialog pD1 = ProgressDialog.show(ActivityControl.this, ActivityControl.this.getString(R.string.sending), ActivityControl.this.getString(R.string.wait_info), true);
            pD1.setCancelable(false);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        byte[] MessageLength = {0x00, 0x00};
                        byte[] NxtMessage = {0x00, 0x09, 0x00, 0x02, 0x00, 0x00};
                        NxtMessage[2] = (byte) (1);
                        NxtMessage[4] = (byte) (cb.isChecked() ? 0 : 1);
                        MessageLength[0] = (byte) NxtMessage.length;
                        while (using_pipeline) {

                        }
                        using_pipeline = true;
                        nxtIN.write(MessageLength, 0, MessageLength.length);
                        nxtIN.write(NxtMessage, 0, NxtMessage.length);
                        int length = nxtOUT.read() + 256 * nxtOUT.read();

                        for (int i = 0; i < length; i++) {
                            if (i == 2) {
                                final int additional_message = nxtOUT.read();
                                if (removeControlCharacters(Integer.toHexString(additional_message)).toUpperCase().contains("EC")) {
                                    mainHandler.post(new Runnable() {
                                        public void run() {
                                            Toast.makeText(getApplicationContext(), getString(R.string.error_reply), Toast.LENGTH_LONG).show();
                                        }
                                    });
                                } else {
                                    if (additional_message == 0) {
                                        mainHandler.post(new Runnable() {
                                            public void run() {
                                                Toast.makeText(getApplicationContext(), getString(R.string.sent), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    } else {
                                        mainHandler.post(new Runnable() {
                                            public void run() {
                                                String add_mess = Integer.toHexString(additional_message);
                                                if (add_mess.length() == 1) {
                                                    add_mess = add_mess + "0";
                                                }
                                                Toast.makeText(getApplicationContext(), getString(R.string.info_reply) + " 0x" + add_mess.toUpperCase(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                }
                            } else {
                                nxtOUT.read();
                            }
                        }
                        using_pipeline = false;
                    } catch (final IOException IOe) {
                        mainHandler.post(new Runnable() {
                            public void run() {
                                ErrorHandler(IOe.getMessage());
                            }
                        });
                    }
                    pD1.dismiss();
                    //Toast.makeText(getApplicationContext(), getString(R.string.sent), Toast.LENGTH_SHORT).show();
                }
            }).start();
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
        }
    }

    public void startProgram(View v) {
        if (GlobalObjects.mmSocket.isConnected()) {
            final ProgressDialog pD1 = ProgressDialog.show(ActivityControl.this, ActivityControl.this.getString(R.string.sending), ActivityControl.this.getString(R.string.wait_info), true);
            pD1.setCancelable(false);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        byte[] arrby = {0x00, 0x00, 0x42, 0x6f, 0x6e, 0x67, 0x62, 0x6f, 0x6d, 0x61, 0x74, 0x2e, 0x72, 0x78, 0x65, 0x00};
                        byte[] MessageLenth = {0x00, 0x00};
                        MessageLenth[0] = (byte) arrby.length;
                        while (using_pipeline) {

                        }
                        using_pipeline = true;
                        nxtIN.write(MessageLenth, 0, MessageLenth.length);
                        nxtIN.write(arrby, 0, arrby.length);
                        using_pipeline = false;
                        mainHandler.post(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), getString(R.string.sent), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (final IOException IOe) {
                        mainHandler.post(new Runnable() {
                            public void run() {
                                ErrorHandler(IOe.getMessage());
                            }
                        });
                    }
                    pD1.dismiss();
                    //Toast.makeText(getApplicationContext(), getString(R.string.sent), Toast.LENGTH_SHORT).show();
                }
            }).start();
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
        }
    }

    public void throwCandy(View view) {
        if (GlobalObjects.mmSocket.isConnected()) {
            final ProgressDialog pD1 = ProgressDialog.show(ActivityControl.this, ActivityControl.this.getString(R.string.sending), ActivityControl.this.getString(R.string.wait_info), true);
            pD1.setCancelable(false);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        byte[] MessageLength = {0x00, 0x00};
                        byte[] NxtMessage = {0x00, 0x09, 0x00, 0x02, 0x00, 0x00};
                        NxtMessage[2] = (byte) (2);
                        NxtMessage[4] = (byte) (1);
                        MessageLength[0] = (byte) NxtMessage.length;
                        while (using_pipeline) {

                        }
                        using_pipeline = true;
                        nxtIN.write(MessageLength, 0, MessageLength.length);
                        nxtIN.write(NxtMessage, 0, NxtMessage.length);
                        int length = nxtOUT.read() + 256 * nxtOUT.read();
                        for (int i = 0; i < length; i++) {
                            if (i == 2) {
                                final int additional_message = nxtOUT.read();
                                if (removeControlCharacters(Integer.toHexString(additional_message)).toUpperCase().contains("EC")) {
                                    mainHandler.post(new Runnable() {
                                        public void run() {
                                            Toast.makeText(getApplicationContext(), getString(R.string.error_reply), Toast.LENGTH_LONG).show();
                                        }
                                    });
                                } else {
                                    if (additional_message == 0) {
                                        mainHandler.post(new Runnable() {
                                            public void run() {
                                                Toast.makeText(getApplicationContext(), getString(R.string.sent), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    } else {
                                        mainHandler.post(new Runnable() {
                                            public void run() {
                                                String add_mess = Integer.toHexString(additional_message);
                                                if (add_mess.length() == 1) {
                                                    add_mess = add_mess + "0";
                                                }
                                                Toast.makeText(getApplicationContext(), getString(R.string.info_reply) + " 0x" + add_mess.toUpperCase(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                }
                            } else {
                                nxtOUT.read();
                            }
                        }
                        using_pipeline = false;

                    } catch (final IOException IOe) {
                        mainHandler.post(new Runnable() {
                            public void run() {
                                ErrorHandler(IOe.getMessage());
                            }
                        });
                    }
                    pD1.dismiss();
                    //Toast.makeText(getApplicationContext(), getString(R.string.sent), Toast.LENGTH_SHORT).show();
                }
            }).start();
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
        }
    }

    public String removeControlCharacters(String s) {
        return s.replaceAll("[\u0000-\u001f]", "");
    }

    public class updateCoins extends Thread {
        public void run() {
            tstopped = false;
            byte[] MessageLength = {0x00, 0x00};
            byte[] readCommand = {0x00, 0x13, 0x0A, 0x00, 0x01};
            MessageLength[0] = (byte) readCommand.length;
            while (readCoins) {
                try {
                    while (using_pipeline) {

                    }
                    using_pipeline = true;
                    nxtIN.write(MessageLength, 0, MessageLength.length);
                    nxtIN.write(readCommand, 0, readCommand.length);
                    int length = nxtOUT.read() + 256 * nxtOUT.read();
                    byte[] number = new byte[65];
                    for (int i = 0; i < length; i++) {
                        if (i > 0) {
                            number[i - 0] = (byte) nxtOUT.read();
                        } else {
                            nxtOUT.read();
                        }
                    }
                    using_pipeline = false;
                    if (!String.format("%02X", number[4]).contains("00")) {
                        sn1 = new String(number);
                        StringBuilder sb = new StringBuilder();
                        for (byte b : number) {
                            sb.append(String.format("%02X ", b));
                        }
                        coinnumber = sn1;
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                tvcoins.setText(coinnumber);
                            }
                        });
                    }

                } catch (Exception e) {
                    final String ex = e.getMessage();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ActivityControl.this, "Error: " + ex, Toast.LENGTH_SHORT).show();
                        }
                    });
                    using_pipeline = false;
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }
            }
            tstopped = true;
        }
    }
}
