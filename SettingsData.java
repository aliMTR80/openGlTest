package com.okm.roveruc;

import android.app.Activity;
import android.content.SharedPreferences;

/* compiled from: Settings.java */
/* loaded from: classes.dex */
class SettingsData {
    private Activity mParentActivity;
    public boolean ScanMode_ZigZag = true;
    public boolean ImpulseMode_Automatic = true;
    public int Impulses = 20;
    public String BluetoothAddress = "00:00:00:00:00:00";
    public String ProbeVersion = "";
    public String LangCode = "";
    public String ActivationCode = "";
    public String SerialNumber = "";
    public boolean isLicenseOKM = false;
    public boolean isLicenseAndroidMarket = false;

    public SettingsData(Activity ParentActivity) {
        this.mParentActivity = null;
        this.mParentActivity = ParentActivity;
    }

    public void LoadFromFile() {
        SharedPreferences settings = this.mParentActivity.getSharedPreferences("okm_settings", 0);
        this.BluetoothAddress = settings.getString("bluetooth_address", "00:00:00:00:00:00");
        this.ScanMode_ZigZag = settings.getBoolean("scanmode_zigzag", true);
        this.ImpulseMode_Automatic = settings.getBoolean("impulsemode_automatic", true);
        this.Impulses = settings.getInt("impulses", 20);
        this.ProbeVersion = settings.getString("probe_version", "");
        this.LangCode = settings.getString("lang_code", "");
        this.ActivationCode = settings.getString("activation_code", "");
        this.SerialNumber = settings.getString("serial_number", "");
        this.isLicenseOKM = settings.getBoolean("isLicenseOKM", false);
        this.isLicenseAndroidMarket = settings.getBoolean("isLicenseAndroidMarket", false);
    }

    public void SaveToFile() {
        SharedPreferences settings = this.mParentActivity.getSharedPreferences("okm_settings", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("bluetooth_address", this.BluetoothAddress);
        editor.putBoolean("scanmode_zigzag", this.ScanMode_ZigZag);
        editor.putBoolean("impulsemode_automatic", this.ImpulseMode_Automatic);
        editor.putInt("impulses", this.Impulses);
        editor.putString("probe_version", this.ProbeVersion);
        editor.putString("lang_code", this.LangCode);
        editor.putString("activation_code", this.ActivationCode);
        editor.putString("serial_number", this.SerialNumber);
        editor.putBoolean("isLicenseOKM", this.isLicenseOKM);
        editor.putBoolean("isLicenseAndroidMarket", this.isLicenseAndroidMarket);
        editor.commit();
    }

    public int setImpulses(int value) {
        this.Impulses = Math.min(Math.abs(value), 100);
        return this.Impulses;
    }
}
