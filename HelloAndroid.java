package com.novinsadr.myapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.SoundPool;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.novinsadr.myapplication.ScanDataObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/* loaded from: classes.dex */
public class HelloAndroid extends Activity implements Handler.Callback, SensorEventListener {
    static final int DIALOG_BLUETOOTH_CONNECTING = 1;
    static final int DIALOG_BLUETOOTH_DISCOVERING = 0;
    static final int DIALOG_SAVE_ERROR = 3;
    static final int DIALOG_SAVE_SUCCESS = 2;
    static final int MSG_HIDE_DIALOG_BLUETOOTH_CONNECTING = 8;
    static final int MSG_HIDE_DIALOG_BLUETOOTH_SCANNING = 1;
    static final int MSG_HIDE_DIALOG_GROUNDSCAN_NEXTLINE = 4;
    static final int MSG_HIDE_DIALOG_GROUNDSCAN_SAVE = 6;
    static final int MSG_SHOW_DIALOG_BLUETOOTH_CONNECTING = 7;
    static final int MSG_SHOW_DIALOG_BLUETOOTH_DISCOVERY_FINISHED = 2;
    static final int MSG_SHOW_DIALOG_BLUETOOTH_FAILED = 9;
    static final int MSG_SHOW_DIALOG_BLUETOOTH_SCANNING = 0;
    static final int MSG_SHOW_DIALOG_GROUNDSCAN_NEXTLINE = 3;
    static final int MSG_SHOW_DIALOG_GROUNDSCAN_SAVE = 5;
    static final int PIXELWIDTH_BUTTON_AREA = 70;
    static final int PIXELWIDTH_OUTER_ROTATION = 80;
    private static final int REQUEST_ENABLE_BT = 2;
    private UUID MY_UUID;
    Button button;
    private EditText edit;
    private GLSurfaceView mGLSurfaceView;
    private float mLastTouchX;
    private float mLastTouchY;
    private ScanRenderer3D mRenderer;
    private ProgressDialog progressDialog;
    Handler mHandler = null;
    byte OperatingMode = -1;
    boolean isAborted = false;
    String CurrentFileName = "";
    float DemoValue = 0.0f;
    float DemoDir = 1.0f;
    long DemoTimeCurrent = 0;
    long DemoTimeLast = 0;
    AlertDialog mAlertDialog = null;
    SettingsData mSettingsData = null;
    ConnectThread mConnectThread = null;
    ArrayList<String> mmBluetoothAddresses = new ArrayList<>();
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothDevice mBluetoothDevice = null;
    private int TouchMode = -1;
    Vibrator mVibrator = null;
    Bundle MyBundle = null;
    private boolean EnableBluetooth = false;
    ScanDataObject mScanDataObject = null;
    private boolean DiscoveryStarted = false;
    private SoundPool mSoundPool = null;
    private int mShortBeepID = 0;
    private LocationManager mLocationManager = null;
    private SensorManager mSensorManager = null;
    private Sensor mMagneticSensor = null;
    private float DemoSensorValue = 0.0f;
    private boolean DemoSensorValid = false;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.okm.roveruc.HelloAndroid.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.bluetooth.device.action.FOUND".equals(action)) {
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                if (device != null) {
                    if (ActivityCompat.checkSelfPermission(HelloAndroid.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    String Name = device.getName();
                    if (Name.equals("OKM Rover UC")) {
                        HelloAndroid.this.mmBluetoothAddresses.add(device.getAddress());
                        return;
                    }
                    return;
                }
                return;
            }
            if ("android.bluetooth.adapter.action.DISCOVERY_STARTED".equals(action)) {
                HelloAndroid.this.DiscoveryStarted = true;
                return;
            }
            if ("android.bluetooth.adapter.action.DISCOVERY_FINISHED".equals(action)) {
                if (HelloAndroid.this.DiscoveryStarted) {
                    HelloAndroid.this.DiscoveryStarted = false;
                    HelloAndroid.this.mHandler.sendEmptyMessage(2);
                    return;
                }
                return;
            }
            if ("android.bluetooth.adapter.action.STATE_CHANGED".equals(action)) {
                int newState = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", 10);
                if (newState == 12) {
                    HelloAndroid.this.mConnectThread = null;
                    HelloAndroid.this.mConnectThread = HelloAndroid.this.new ConnectThread(HelloAndroid.this.mBluetoothDevice, HelloAndroid.this, HelloAndroid.this.mHandler, HelloAndroid.this.mmBluetoothAddresses);
                    HelloAndroid.this.mConnectThread.setScanDataObject(HelloAndroid.this.mScanDataObject);
                    HelloAndroid.this.mConnectThread.setDiscovery(true);
                    HelloAndroid.this.mConnectThread.start();
                }
            }
        }
    };
    View.OnTouchListener mOnTouchListener = new View.OnTouchListener() { // from class: com.okm.roveruc.HelloAndroid.2
        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.view.View.OnTouchListener
        public boolean onTouch(View v, MotionEvent event) {
            if (HelloAndroid.this.mScanDataObject != null) {
                int action = event.getAction();
                float x = event.getX();
                float y = event.getY();
                switch (action) {
                    case 0:
                        HelloAndroid.this.mLastTouchX = x;
                        HelloAndroid.this.mLastTouchY = y;
                        if (HelloAndroid.this.IsButtonArea(x, y)) {
                            HelloAndroid.this.TouchMode = -1;
                            HelloAndroid.this.mLastTouchX = Float.NaN;
                            HelloAndroid.this.mLastTouchY = Float.NaN;
                            if (HelloAndroid.this.mScanDataObject.ScreenWidth() < HelloAndroid.this.mScanDataObject.ScreenHeight()) {
                                float dist = HelloAndroid.this.mScanDataObject.ScreenWidth() / HelloAndroid.MSG_SHOW_DIALOG_GROUNDSCAN_SAVE;
                                if (y > HelloAndroid.this.mScanDataObject.ScreenHeight() - 65) {
                                    if (x > (1.0f * dist) - 32.0f && x < (1.0f * dist) + 32.0f) {
                                        HelloAndroid.this.mScanDataObject.setTransformationMode((byte) 0);
                                    } else if (x > (2.0f * dist) - 32.0f && x < (2.0f * dist) + 32.0f) {
                                        HelloAndroid.this.mScanDataObject.setTransformationMode((byte) 1);
                                    } else if (x > (3.0f * dist) - 32.0f && x < (3.0f * dist) + 32.0f) {
                                        HelloAndroid.this.mScanDataObject.setTransformationMode((byte) 4);
                                    }
                                    if (x > (4.0f * dist) - 32.0f && x < (4.0f * dist) + 32.0f) {
                                        HelloAndroid.this.mScanDataObject.SwitchView();
                                        break;
                                    }
                                }
                            } else {
                                float dist2 = HelloAndroid.this.mScanDataObject.ScreenHeight() / HelloAndroid.MSG_SHOW_DIALOG_GROUNDSCAN_SAVE;
                                if (x < 65.0f) {
                                    float y2 = HelloAndroid.this.mScanDataObject.ScreenHeight() - y;
                                    if (y2 > (1.0f * dist2) - 32.0f && y2 < (1.0f * dist2) + 32.0f) {
                                        HelloAndroid.this.mScanDataObject.setTransformationMode((byte) 0);
                                    } else if (y2 > (2.0f * dist2) - 32.0f && y2 < (2.0f * dist2) + 32.0f) {
                                        HelloAndroid.this.mScanDataObject.setTransformationMode((byte) 1);
                                    } else if (y2 > (3.0f * dist2) - 32.0f && y2 < (3.0f * dist2) + 32.0f) {
                                        HelloAndroid.this.mScanDataObject.setTransformationMode((byte) 4);
                                    }
                                    if (y2 > (4.0f * dist2) - 32.0f && y2 < (4.0f * dist2) + 32.0f) {
                                        HelloAndroid.this.mScanDataObject.SwitchView();
                                        break;
                                    }
                                }
                            }
                        } else if (HelloAndroid.this.IsOuterFrameTop(x, y)) {
                            HelloAndroid.this.TouchMode = 1;
                            break;
                        } else if (HelloAndroid.this.IsOuterFrameBottom(x, y)) {
                            HelloAndroid.this.TouchMode = 3;
                            break;
                        } else if (HelloAndroid.this.IsOuterFrameRight(x, y)) {
                            HelloAndroid.this.TouchMode = 2;
                            break;
                        } else if (HelloAndroid.this.IsOuterFrameLeft(x, y)) {
                            HelloAndroid.this.TouchMode = 0;
                            break;
                        } else {
                            HelloAndroid.this.TouchMode = HelloAndroid.MSG_HIDE_DIALOG_GROUNDSCAN_NEXTLINE;
                            break;
                        }
                        break;
                    case 1:
                        HelloAndroid.this.TouchMode = -1;
                        HelloAndroid.this.mLastTouchX = Float.NaN;
                        HelloAndroid.this.mLastTouchY = Float.NaN;
                        break;
                    case 2:
                        if (!Float.isNaN(HelloAndroid.this.mLastTouchX) && !Float.isNaN(HelloAndroid.this.mLastTouchY)) {
                            float dx = x - HelloAndroid.this.mLastTouchX;
                            float dy = y - HelloAndroid.this.mLastTouchY;
                            HelloAndroid.this.mScanDataObject.TransformBy(dx, dy, HelloAndroid.this.TouchMode);
                            HelloAndroid.this.mLastTouchX = x;
                            HelloAndroid.this.mLastTouchY = y;
                            break;
                        }
                        break;
                }
            }
            return true;
        }
    };
    InputFilter filter = new InputFilter() { // from class: com.okm.roveruc.HelloAndroid.3
        @Override // android.text.InputFilter
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            for (int i = start; i < end; i++) {
                if (!Character.isLetterOrDigit(source.charAt(i)) && source.charAt(i) != '-' && source.charAt(i) != '_' && source.charAt(i) != '.') {
                    return "";
                }
            }
            return null;
        }
    };

    @Override // android.app.Activity, android.content.ComponentCallbacks
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        SelectLanguage.ChangeLanguage(getBaseContext(), this.mSettingsData.LangCode, null);
    }

    private void InitBluetooth() {
        if (this.EnableBluetooth) {
            this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (this.mBluetoothAdapter == null) {
                Toast.makeText(this, getString(R.string.Bluetooth_NotAvailable), Toast.LENGTH_LONG).show();
                setResult(0);
                finish();
                return;
            }
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            this.mBluetoothAdapter.setName("OKM-Rover-UC");
            if (!BluetoothAdapter.checkBluetoothAddress(this.mSettingsData.BluetoothAddress)) {
                this.mSettingsData.BluetoothAddress = "00:00:00:00:00:00";
            }
            if (!this.mSettingsData.BluetoothAddress.equals("00:00:00:00:00:00")) {
                this.mBluetoothDevice = this.mBluetoothAdapter.getRemoteDevice(this.mSettingsData.BluetoothAddress);
                this.mmBluetoothAddresses.add(this.mSettingsData.BluetoothAddress);
            }
            registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.device.action.FOUND"));
            registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.adapter.action.DISCOVERY_STARTED"));
            registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.adapter.action.DISCOVERY_FINISHED"));
            registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED"));
            if (!this.mBluetoothAdapter.isEnabled()) {
                new AlertDialog.Builder(this).setTitle(R.string.MessageBox_Bluetooth).setMessage(R.string.Bluetooth_TurnOn).setPositiveButton(R.string.Button_Yes, new DialogInterface.OnClickListener() { // from class: com.okm.roveruc.HelloAndroid.4
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int which) {
                        if (ActivityCompat.checkSelfPermission(HelloAndroid.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        if (!HelloAndroid.this.mBluetoothAdapter.enable()) {
                            Intent enableIntent = new Intent("android.bluetooth.adapter.action.REQUEST_ENABLE");
                            HelloAndroid.this.startActivityForResult(enableIntent, 2);
                        }
                    }
                }).setCancelable(false).setNegativeButton(R.string.Button_No, new DialogInterface.OnClickListener() { // from class: com.okm.roveruc.HelloAndroid.5
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int which) {
                        HelloAndroid.this.onBackPressed();
                    }
                }).show();
                return;
            }
            this.mConnectThread = null;
            this.mConnectThread = new ConnectThread(this.mBluetoothDevice, this, this.mHandler, this.mmBluetoothAddresses);
            this.mConnectThread.setScanDataObject(this.mScanDataObject);
            this.mConnectThread.setDiscovery(true);
            this.mConnectThread.start();
        }
    }

    @SuppressLint("WrongConstant")
    @Override // android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mSettingsData = new SettingsData(this);
        this.mSettingsData.LoadFromFile();
        this.mHandler = new Handler(this);
        this.mVibrator = (Vibrator) getSystemService("vibrator");
        this.mLocationManager = (LocationManager) getSystemService("location");
        this.mSoundPool = new SoundPool(2, 3, 0);
        setVolumeControlStream(3);
        Intent startIntent = getIntent();
        this.MyBundle = startIntent.getExtras();
        this.OperatingMode = (byte) -1;
        if (this.MyBundle != null) {
            this.OperatingMode = this.MyBundle.getByte("opmode");
            this.EnableBluetooth = this.MyBundle.getBoolean("bluetooth");
        }
        this.mScanDataObject = new ScanDataObject(this.OperatingMode, getApplicationContext());
        this.mScanDataObject.setImpulses(this.mSettingsData.Impulses);
        this.mScanDataObject.setZigZag(this.mSettingsData.ScanMode_ZigZag);
        this.mScanDataObject.isAutomatic(this.mSettingsData.ImpulseMode_Automatic);
        setRequestedOrientation(MSG_HIDE_DIALOG_GROUNDSCAN_NEXTLINE);
        this.mRenderer = new ScanRenderer3D();
        this.mRenderer.setScanDataObject(this.mScanDataObject);
        this.mGLSurfaceView = new GLSurfaceView(this);
        this.mGLSurfaceView.setRenderer(this.mRenderer);
        this.mGLSurfaceView.setOnTouchListener(this.mOnTouchListener);
        setContentView(this.mGLSurfaceView);
        String MyFilename = this.MyBundle.getString("filename");
        if (MyFilename != null) {
            this.mScanDataObject.OpenFromFile(MyFilename);
        }
    }

    @Override // android.app.Activity
    protected void onStart() {
        super.onStart();
        InitBluetooth();
        onResume();
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        super.onDestroy();
        if (this.mVibrator != null) {
            this.mVibrator.cancel();
            this.mVibrator = null;
        }
        if (this.mSoundPool != null) {
            this.mSoundPool = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean IsOuterFrameLeft(float x, float y) {
        return this.mScanDataObject.ScreenWidth() < this.mScanDataObject.ScreenHeight() ? x < 80.0f : x > 70.0f && x < 150.0f;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean IsOuterFrameRight(float x, float y) {
        return this.mScanDataObject.ScreenWidth() < this.mScanDataObject.ScreenHeight() ? x > ((float) (this.mScanDataObject.ScreenWidth() + (-80))) : x > ((float) (this.mScanDataObject.ScreenWidth() + (-80)));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean IsOuterFrameBottom(float x, float y) {
        return this.mScanDataObject.ScreenWidth() < this.mScanDataObject.ScreenHeight() ? y > ((float) ((this.mScanDataObject.ScreenHeight() + (-70)) + (-80))) : y > ((float) (this.mScanDataObject.ScreenHeight() + (-80)));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean IsOuterFrameTop(float x, float y) {
        return this.mScanDataObject.ScreenWidth() < this.mScanDataObject.ScreenHeight() ? y < 80.0f : y < 80.0f;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean IsButtonArea(float x, float y) {
        return this.mScanDataObject.ScreenWidth() < this.mScanDataObject.ScreenHeight() ? y > ((float) (this.mScanDataObject.ScreenHeight() + (-70))) : x < 70.0f;
    }

    @Override // android.app.Activity
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 2:
                if (resultCode == -1) {
                    this.mConnectThread = null;
                    this.mConnectThread = new ConnectThread(this.mBluetoothDevice, this, this.mHandler, this.mmBluetoothAddresses);
                    this.mConnectThread.setScanDataObject(this.mScanDataObject);
                    this.mConnectThread.setDiscovery(true);
                    this.mConnectThread.start();
                    break;
                } else {
                    setResult(0);
                    finish();
                    break;
                }
        }
    }

    @Override // android.app.Activity
    public boolean onCreateOptionsMenu(Menu menu) {
        if (this.OperatingMode == 1) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.example_menu, menu);
            menu.getItem(0).setIcon(R.drawable.ic_menu_compose);
            menu.getItem(0).setTitle(R.string.GroundScanMenu_New);
            menu.getItem(1).setIcon(R.drawable.ic_menu_archive);
            menu.getItem(1).setTitle(R.string.GroundScanMenu_Open);
            menu.getItem(2).setIcon(R.drawable.ic_menu_save);
            menu.getItem(2).setTitle(R.string.GroundScanMenu_Save);
            menu.getItem(2).setEnabled(this.mScanDataObject.canSave());
            menu.getItem(3).setIcon(R.drawable.ic_menu_revert);
            menu.getItem(3).setTitle(R.string.GroundScanMenu_Reset);
        }
        return true;
    }

    @Override // android.app.Activity
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        int itemId = item.getItemId();

        if (itemId == R.id.item01) {
            Intent startIntent = new Intent(this, Settings.class);
            Bundle startBundle = new Bundle();
            startBundle.putByte("caller", (byte) 4);
            startIntent.putExtras(startBundle);
            startActivity(startIntent);
            finish();
            return true;
        }
        else if (itemId == R.id.item06) {
            Intent startIntent2 = new Intent(this, FileExplorer.class);
            Bundle startBundle2 = new Bundle();
            startBundle2.putByte("caller", (byte) 4);
            startIntent2.putExtras(startBundle2);
            startActivity(startIntent2);
            finish();
            return true;
        }
        else if (itemId == R.id.item05) {
            this.mHandler.sendEmptyMessage(999);
            return true;
        }
        else if (itemId == R.id.item04) {
            this.mScanDataObject.ResetTransformation();
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }
    @Override // android.app.Activity
    public void onBackPressed() {
        super.onBackPressed();
        this.isAborted = true;
        Intent startIntent = new Intent(this, (Class<?>) MainMenu.class);
        startIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startIntent.putExtra("Lcheck", false);
        startActivity(startIntent);
        finish();
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        this.mGLSurfaceView.onResume();
        registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.device.action.FOUND"));
        registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.adapter.action.DISCOVERY_STARTED"));
        registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.adapter.action.DISCOVERY_FINISHED"));
        registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED"));
        if (this.mSoundPool != null) {
            this.mShortBeepID = this.mSoundPool.load(this, R.raw.sbeep, 1);
        }
    }

    @Override // android.app.Activity
    protected void onPause() {
        super.onPause();
        this.mGLSurfaceView.onPause();
        if (this.mSoundPool == null || this.mShortBeepID <= 0) {
            return;
        }
        this.mSoundPool.unload(this.mShortBeepID);
    }

    private class ConnectThread extends Thread {
        static final byte BTN_1 = -2;
        static final byte BTN_STATE_DOWN = 1;
        static final byte BTN_STATE_PRESSED = 2;
        static final byte BTN_STATE_UP = 0;
        private float SensorValue;
        private boolean isConnected;
        private byte ButtonState = 0;
        private BluetoothSocket mmSocket = null;
        private InputStream mmInStream = null;
        private OutputStream mmOutStream = null;
        private boolean doDiscovery = true;
        ScanDataObject mScanDataObject = null;

        public synchronized void setScanDataObject(ScanDataObject mObject) {
            this.mScanDataObject = mObject;
        }

        public ConnectThread(BluetoothDevice device, Activity Parent, Handler h, ArrayList<String> BluetoothAddresses) {
            this.isConnected = false;
            this.isConnected = false;
        }

        public synchronized void setDiscovery(boolean DoDiscovery) {
            this.doDiscovery = DoDiscovery;
        }

        private synchronized void DiscoverBluetoothDevices() {
            HelloAndroid.this.mHandler.sendEmptyMessage(2);
        }

        private synchronized void ConnectToBluetoothProbe() {
            ConnectToBluetoothProbe(HelloAndroid.this.mSettingsData.BluetoothAddress);
        }

        private synchronized boolean ConnectToBluetoothProbe(String MyBluetoothAddress) {
            boolean z = true;
            synchronized (this) {
                if (!BluetoothAdapter.checkBluetoothAddress(MyBluetoothAddress) || MyBluetoothAddress.equals("00:00:00:00:00:00")) {
                    z = false;
                } else {
                    HelloAndroid.this.mBluetoothDevice = null;
                    HelloAndroid.this.mBluetoothDevice = HelloAndroid.this.mBluetoothAdapter.getRemoteDevice(MyBluetoothAddress);
                    HelloAndroid.this.MY_UUID = new UUID(0L, 0L);
                    HelloAndroid.this.MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                    try {
                        if (ActivityCompat.checkSelfPermission(HelloAndroid.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return false;
                        }
                        BluetoothSocket tmp = HelloAndroid.this.mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(HelloAndroid.this.MY_UUID);
                        try {
                            this.mmSocket = tmp;
                            try {
                                sleep(500L);
                            } catch (InterruptedException e) {
                            }
                            this.mmSocket.connect();
                            try {
                                this.mmInStream = this.mmSocket.getInputStream();
                                this.mmOutStream = this.mmSocket.getOutputStream();
                                this.isConnected = true;
                                HelloAndroid.this.mSettingsData.ProbeVersion = GetVersion();
                                HelloAndroid.this.mSettingsData.SaveToFile();
                            } catch (IOException e2) {
                                z = false;
                            }
                        } catch (IOException e3) {
                            try {
                                this.mmSocket.close();
                            } catch (IOException e4) {
                            }
                            z = false;
                        }
                    } catch (IOException e5) {
                        z = false;
                    }
                }
            }
            return z;
        }

        private synchronized float readSensorValue(InputStream stream) throws IOException {
            float result;
            result = Float.NaN;
            if (stream != null) {
                int tmp = stream.read();
                if (tmp >= 0) {
                    int value = tmp << 24;
                    int tmp2 = stream.read();
                    if (tmp2 >= 0) {
                        int value2 = value + (tmp2 << 16);
                        int tmp3 = stream.read();
                        if (tmp3 >= 0) {
                            result = (value2 + (tmp3 << HelloAndroid.MSG_HIDE_DIALOG_BLUETOOTH_CONNECTING)) / 256;
                        }
                    }
                }
            }
            return result;
        }

        private synchronized String GetVersion() {
            String Version;
            Version = "";
            if (this.mmOutStream != null && this.mmInStream != null) {
                try {
                    this.mmOutStream.write(15);
                    this.mmOutStream.flush();
                    while (true) {
                        int tmp = this.mmInStream.read();
                        if (tmp <= 0) {
                            break;
                        }
                        if (((char) tmp) != 'V') {
                            Version = String.valueOf(Version) + ((char) tmp);
                        }
                    }
                } catch (IOException e) {
                }
            }
            return Version;
        }

        private synchronized void UpdateButtonStatus() {
            int ButtonStatus = 255;
            if (this.mmOutStream != null && this.mmInStream != null) {
                try {
                    this.mmOutStream.write(12);
                    this.mmOutStream.flush();
                    int tmp = this.mmInStream.read();
                    if (tmp >= 0) {
                        ButtonStatus = tmp;
                    }
                } catch (IOException e) {
                }
            }
            boolean ButtonDown = (ButtonStatus | (-2)) == -2;
            if (ButtonDown && this.ButtonState == 0) {
                this.ButtonState = (byte) 2;
            } else if (!ButtonDown && this.ButtonState == 1) {
                this.ButtonState = (byte) 0;
            }
        }

        private synchronized float getDiffSensorExt(float Correction) {
            float Diff;
            Diff = Float.NaN;
            if (this.mmOutStream != null && this.mmInStream != null) {
                try {
                    this.mmOutStream.write(14);
                    this.mmOutStream.flush();
                    float Top = readSensorValue(this.mmInStream);
                    float Bottom = readSensorValue(this.mmInStream);
                    if (!Float.isNaN(Top) && !Float.isNaN(Bottom)) {
                        Diff = -(Top - (-Bottom));
                        if (Correction >= 0.0f) {
                            Diff = (100.0f * (Diff < 0.0f ? (float) (-Math.log1p(Math.abs(Diff))) : (float) Math.log1p(Diff))) + Correction;
                        }
                    }
                } catch (IOException e) {
                    try {
                        this.mmOutStream = this.mmSocket.getOutputStream();
                        this.mmInStream = this.mmSocket.getInputStream();
                    } catch (IOException e2) {
                    }
                }
            }
            return Diff;
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            super.run();
            long LastTime = 0;
            boolean DialogVisible = false;
            HelloAndroid.this.isAborted = false;
            double quality = 0.0d;
            double longitude = 0.0d;
            double latitude = 0.0d;
            HelloAndroid.this.mHandler.sendEmptyMessage(0);
            ConnectToBluetoothProbe();
            if (!this.isConnected) {
                if (this.doDiscovery) {
                    DiscoverBluetoothDevices();
                } else {
                    HelloAndroid.this.mHandler.sendEmptyMessage(HelloAndroid.MSG_SHOW_DIALOG_BLUETOOTH_FAILED);
                }
            } else {
                try {
                    sleep(2000L);
                } catch (InterruptedException e) {
                }
                HelloAndroid.this.mHandler.sendEmptyMessage(1);
                this.mScanDataObject.InitGrid();
                this.mScanDataObject.ScanActive = true;
                if (HelloAndroid.this.mReceiver != null) {
                    HelloAndroid.this.unregisterReceiver(HelloAndroid.this.mReceiver);
                }
            }
            while (this.isConnected && this.mmSocket != null && !HelloAndroid.this.isAborted) {
                UpdateButtonStatus();
                if (HelloAndroid.this.OperatingMode == 1) {
                    if (this.mScanDataObject.UserActionRequired() < 0) {
                        DialogVisible = false;
                        this.SensorValue = getDiffSensorExt(65000.0f);
                        if (!Float.isNaN(this.SensorValue)) {
                            if (HelloAndroid.this.mLocationManager != null) {
                                if (ActivityCompat.checkSelfPermission(HelloAndroid.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(HelloAndroid.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    // TODO: Consider calling
                                    //    ActivityCompat#requestPermissions
                                    // here to request the missing permissions, and then overriding
                                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                    //                                          int[] grantResults)
                                    // to handle the case where the user grants the permission. See the documentation
                                    // for ActivityCompat#requestPermissions for more details.
                                    return;
                                }
                                Location location = HelloAndroid.this.mLocationManager.getLastKnownLocation("gps");
                                if (HelloAndroid.this.mLocationManager.isProviderEnabled("gps") && location != null) {
                                    quality = location.getAccuracy();
                                    longitude = location.getLongitude();
                                    latitude = location.getLatitude();
                                } else {
                                    quality = 0.0d;
                                    longitude = 0.0d;
                                    latitude = 0.0d;
                                }
                            }
                            long CurrentTime = System.nanoTime();
                            if ((this.mScanDataObject.isAutomatic() && CurrentTime - 250000000 > LastTime) || (!this.mScanDataObject.isAutomatic() && this.ButtonState == 2)) {
                                if (this.mScanDataObject.GroundScan_AddValue(this.SensorValue, longitude, latitude, quality) == 0) {
                                    if (HelloAndroid.this.mShortBeepID > 0 && this.mScanDataObject.UserActionRequired() < 0) {
                                        int tmp = HelloAndroid.this.mSoundPool.play(HelloAndroid.this.mShortBeepID, 0.9f, 0.9f, 0, 0, 2.0f);
                                        int i = (tmp - 3) + tmp;
                                    }
                                } else {
                                    HelloAndroid.this.mVibrator.vibrate(20L);
                                }
                                LastTime = CurrentTime;
                                if (this.ButtonState == 2) {
                                    this.ButtonState = (byte) 1;
                                }
                            }
                        }
                    } else if (!DialogVisible) {
                        if (HelloAndroid.this.mShortBeepID > 0 && !this.mScanDataObject.isFirstValue) {
                            HelloAndroid.this.mSoundPool.play(HelloAndroid.this.mShortBeepID, 1.0f, 1.0f, 0, 2, 2.0f);
                        }
                        HelloAndroid.this.mHandler.sendEmptyMessage(3);
                        DialogVisible = true;
                    } else if (this.ButtonState == 2) {
                        this.ButtonState = (byte) 1;
                        HelloAndroid.this.mHandler.sendEmptyMessage(HelloAndroid.MSG_HIDE_DIALOG_GROUNDSCAN_NEXTLINE);
                    }
                } else if (HelloAndroid.this.OperatingMode == 0 || HelloAndroid.this.OperatingMode == 2) {
                    long CurrentTime2 = System.nanoTime();
                    this.SensorValue = getDiffSensorExt(-1.0f);
                    if (this.ButtonState == 2) {
                        this.mScanDataObject.AddValue(Float.NaN);
                        this.ButtonState = (byte) 1;
                    } else if (!Float.isNaN(this.SensorValue) && CurrentTime2 - 30000000 > LastTime) {
                        this.mScanDataObject.AddValue(this.SensorValue);
                        LastTime = CurrentTime2;
                    }
                }
            }
            Disconnect();
        }

        private synchronized void Disconnect() {
            this.isConnected = false;
            if (this.mmOutStream != null) {
                try {
                    this.mmOutStream.write(99);
                    sleep(10L);
                    this.mmOutStream.close();
                } catch (Exception e) {
                }
                this.mmOutStream = null;
            }
            if (this.mmInStream != null) {
                try {
                    this.mmInStream.close();
                } catch (Exception e2) {
                }
                this.mmInStream = null;
            }
            if (this.mmSocket != null) {
                try {
                    this.mmSocket.close();
                } catch (Exception e3) {
                }
                this.mmSocket = null;
            }
        }
    }

    @Override // android.app.Activity
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case 0:
                this.progressDialog = new ProgressDialog(this);
                this.progressDialog.setMessage(getString(R.string.Bluetooth_Discovering));
                this.progressDialog.isIndeterminate();
                this.progressDialog.setCancelable(true);
                this.progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() { // from class: com.okm.roveruc.HelloAndroid.6
                    @Override // android.content.DialogInterface.OnCancelListener
                    public void onCancel(DialogInterface dialog) {
                        HelloAndroid.this.onBackPressed();
                    }
                });
                return this.progressDialog;
            default:
                return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean SaveToFile(String aFileName) {
        return this.mScanDataObject.SaveToFile(aFileName);
    }

    private void ShowMessageBox(boolean isInfo, String Title, String Message) {
        int IconID = android.R.drawable.ic_dialog_info;
        if (!isInfo) {
            IconID = android.R.drawable.ic_dialog_alert;
        }
        this.mAlertDialog = new AlertDialog.Builder(this).setIcon(IconID).setTitle(Title).setMessage(Message).setCancelable(false).setPositiveButton(R.string.Button_OK, new DialogInterface.OnClickListener() { // from class: com.okm.roveruc.HelloAndroid.7
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int which) {
            }
        }).show();
    }

    private void ShowYesNoBox(int IconID, View layout, String Title, String Message, DialogInterface.OnClickListener YesAction, DialogInterface.OnClickListener NoAction) {
        if (IconID < 0) {
            IconID = android.R.drawable.ic_dialog_alert;
        }
        this.mAlertDialog = new AlertDialog.Builder(this).setIcon(IconID).setTitle(Title).setMessage(Message).setCancelable(false).setPositiveButton(R.string.Button_Yes, YesAction).setNegativeButton(R.string.Button_No, NoAction).create();
        if (layout != null) {
            this.mAlertDialog.setView(layout);
        }
        this.mAlertDialog.show();
    }

    @Override // android.os.Handler.Callback
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 0:
                showDialog(0);
                break;
            case 1:
                try {
                    removeDialog(0);
                    break;
                } catch (IllegalArgumentException e) {
                    break;
                }
            case 2:
                try {
                    this.DiscoveryStarted = false;
                    removeDialog(0);
                } catch (IllegalArgumentException e2) {
                }
                this.mConnectThread = null;
                this.mConnectThread = new ConnectThread(this.mBluetoothDevice, this, this.mHandler, this.mmBluetoothAddresses);
                this.mConnectThread.setScanDataObject(this.mScanDataObject);
                this.mConnectThread.setDiscovery(false);
                this.mConnectThread.start();
                break;
            case 3:
                ShowYesNoBox(R.drawable.help, null, getString(R.string.GroundScan_NewLineTitle), getString(R.string.GroundScan_NewLine), new DialogInterface.OnClickListener() { // from class: com.okm.roveruc.HelloAndroid.8
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int which) {
                        HelloAndroid.this.mScanDataObject.Continue();
                    }
                }, new DialogInterface.OnClickListener() { // from class: com.okm.roveruc.HelloAndroid.9
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int which) {
                        HelloAndroid.this.mScanDataObject.ScanActive = false;
                        HelloAndroid.this.isAborted = true;
                        if (HelloAndroid.this.mScanDataObject.canSave()) {
                            HelloAndroid.this.mHandler.sendEmptyMessage(HelloAndroid.MSG_SHOW_DIALOG_GROUNDSCAN_SAVE);
                        }
                    }
                });
                break;
            case MSG_HIDE_DIALOG_GROUNDSCAN_NEXTLINE /* 4 */:
                if (this.mAlertDialog != null && this.mAlertDialog.isShowing()) {
                    this.mAlertDialog.dismiss();
                    this.mScanDataObject.Continue();
                    break;
                }
                break;
            case MSG_SHOW_DIALOG_GROUNDSCAN_SAVE /* 5 */:
                this.isAborted = true;
                ShowYesNoBox(R.drawable.save, null, getString(R.string.GroundScan_SaveScanTitle), getString(R.string.GroundScan_SaveScan), new DialogInterface.OnClickListener() { // from class: com.okm.roveruc.HelloAndroid.12
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int which) {
                        HelloAndroid.this.mHandler.sendEmptyMessage(999);
                    }
                }, null);
                break;
            case MSG_HIDE_DIALOG_GROUNDSCAN_SAVE /* 6 */:
                if (this.mAlertDialog != null && this.mAlertDialog.isShowing()) {
                    this.mAlertDialog.dismiss();
                    break;
                }
                break;
            case MSG_SHOW_DIALOG_BLUETOOTH_CONNECTING /* 7 */:
                showDialog(1);
                break;
            case MSG_HIDE_DIALOG_BLUETOOTH_CONNECTING /* 8 */:
                removeDialog(1);
                break;
            case MSG_SHOW_DIALOG_BLUETOOTH_FAILED /* 9 */:
                try {
                    this.DiscoveryStarted = false;
                    removeDialog(0);
                } catch (IllegalArgumentException e3) {
                }
                ShowYesNoBox(-1, null, getString(R.string.MessageBox_Error), getString(R.string.Bluetooth_ConnectionFailed), new DialogInterface.OnClickListener() { // from class: com.okm.roveruc.HelloAndroid.10
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int which) {
                        HelloAndroid.this.mConnectThread = null;
                        HelloAndroid.this.mConnectThread = HelloAndroid.this.new ConnectThread(HelloAndroid.this.mBluetoothDevice, HelloAndroid.this, HelloAndroid.this.mHandler, HelloAndroid.this.mmBluetoothAddresses);
                        HelloAndroid.this.mConnectThread.setScanDataObject(HelloAndroid.this.mScanDataObject);
                        HelloAndroid.this.mConnectThread.setDiscovery(true);
                        HelloAndroid.this.mConnectThread.start();
                    }
                }, new DialogInterface.OnClickListener() { // from class: com.okm.roveruc.HelloAndroid.11
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int which) {
                        HelloAndroid.this.setResult(0);
                        HelloAndroid.this.finish();
                    }
                });
                break;
            case 500:
                ShowMessageBox(false, getString(R.string.MessageBox_Error), getString(R.string.GroundScan_SavedFileError));
                break;
            case 501:
                ShowMessageBox(true, getString(R.string.MessageBox_Information), getString(R.string.GroundScan_SavedFileSuccess));
                break;
            case 888:
                if (this.mScanDataObject.FileExists(this.CurrentFileName)) {
                    ShowYesNoBox(-1, null, getString(R.string.MessageBox_Confirmation), getString(R.string.GroundScan_OverwriteFile), new DialogInterface.OnClickListener() { // from class: com.okm.roveruc.HelloAndroid.13
                        @Override // android.content.DialogInterface.OnClickListener
                        public void onClick(DialogInterface dialog, int which) {
                            if (HelloAndroid.this.SaveToFile(HelloAndroid.this.edit.getText().toString())) {
                                HelloAndroid.this.mHandler.sendEmptyMessage(501);
                            } else {
                                HelloAndroid.this.mHandler.sendEmptyMessage(500);
                            }
                        }
                    }, null);
                    break;
                } else if (SaveToFile(this.CurrentFileName)) {
                    this.mHandler.sendEmptyMessage(501);
                    break;
                } else {
                    this.mHandler.sendEmptyMessage(500);
                    break;
                }
            case 999:
                Context mContext = getApplicationContext();
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.savedialog, (ViewGroup) findViewById(R.id.layout_root));
                this.edit = (EditText) layout.findViewById(R.id.EditText01);
                this.edit.setText(DateFormat.format("yyyy-MM-dd kk.mm.ss", new Date()));
                this.edit.setFilters(new InputFilter[]{this.filter});
                ShowYesNoBox(R.drawable.save, layout, getString(R.string.GroundScan_SaveScanTitle), getString(R.string.GroundScan_EnterFilename), new DialogInterface.OnClickListener() { // from class: com.okm.roveruc.HelloAndroid.14
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int which) {
                        HelloAndroid.this.CurrentFileName = HelloAndroid.this.edit.getText().toString();
                        HelloAndroid.this.mHandler.sendEmptyMessage(888);
                    }
                }, null);
                break;
            default:
                return false;
        }
        return true;
    }

    @Override // android.hardware.SensorEventListener
    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }

    @Override // android.hardware.SensorEventListener
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == 2) {
            this.DemoSensorValid = true;
            this.DemoSensorValue = (float) Math.sqrt((event.values[0] * event.values[0]) + (event.values[1] * event.values[1]) + (event.values[2] * event.values[2]));
        }
    }
}
