package com.novinsadr.graph.renderer;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DataProcessor {
    private static final String TAG = "DataProcessor";

    public enum Mode {
        SCAN,
        MAGNETOMETER,
        POINT_LOCATOR
    }

    private Mode mode;
    private int scanThreshold;
    private int scanCounter = 0;
    private List<Float> scanValues = new ArrayList<>();
    public static boolean scanReady = false;
    private float basePoint;
    private float currentAverage;
    private boolean isBasePointSet = false;

    // تغییر محدوده‌ی داده‌های ورودی
    private final float DATA_MIN = 0;
    public final float DATA_MAX = 32768;

    // محدوده‌ی جدید برای نرمال‌سازی
    private final float NORMALIZED_MIN = 0.0f;
    public final float NORMALIZED_MAX = 1000.0f;

    public DataProcessor(Mode mode, int scanThreshold) {
        this.mode = mode;
        this.scanThreshold = scanThreshold;
    }

    public DataProcessor(Mode mode) {
        this.mode = mode;
    }

    public float processData(float zValue) {
        switch (mode) {
            case SCAN:
                return processScanData(zValue);
            case MAGNETOMETER:
            case POINT_LOCATOR:
                return processMagnetometerAndPointLocatorData(zValue);
            default:
                return normalizeData(zValue);
        }
    }

    private float processScanData(float zValue) {
        if (!scanReady) {
            scanValues.add(zValue);
            scanCounter++;

            if (scanCounter == scanThreshold) {
                // محاسبه میانگین و نقطه مبنا
                float sum = 0;
                for (float value : scanValues) {
                    sum += value;
                }
                currentAverage = sum / scanThreshold;
                Log.i(TAG, "processScanData: " + currentAverage);
                // نرمال‌سازی نقطه مبنا به مقیاس جدید
                basePoint = normalizeToNewRange(currentAverage);
                scanReady = true;
                //پاک کردن لیست scanValues
                scanValues.clear();
                return processMagnetometerAndPointLocatorData(zValue);
            } else {
                return NORMALIZED_MIN; // مقدار پیش‌فرض در محدوده جدید
            }
        } else {
            return processMagnetometerAndPointLocatorData(zValue);
        }
    }

    private float processMagnetometerAndPointLocatorData(float zValue) {
        if (!isBasePointSet) {
            currentAverage = zValue;
            basePoint = normalizeToNewRange(zValue);
            isBasePointSet = true;
            Log.d(TAG, "Base point set automatically to = " + basePoint);
            return normalizeData(zValue);
        } else {
            return normalizeData(zValue);
        }
    }

    private float normalizeData(float zValue) {
        // نرمال‌سازی به محدوده جدید
        float normalizedValue = normalizeToNewRange(zValue);

        // محدود کردن مقادیر به بازه جدید
        normalizedValue = Math.max(NORMALIZED_MIN, Math.min(NORMALIZED_MAX, normalizedValue));

        Log.d(TAG, String.format("Original: %.2f, Base: %.2f, Normalized: %.2f",
                zValue, basePoint, normalizedValue));

        return normalizedValue;
    }

    // تابع کمکی برای نرمال‌سازی به محدوده جدید
    private float normalizeToNewRange(float value) {
        return (value * NORMALIZED_MAX) / DATA_MAX;
    }

    public float getBasePoint() {
        return basePoint;
    }

    public void setBasePoint(float zValue) {
        this.currentAverage = zValue;
        this.basePoint = normalizeToNewRange(zValue);
        isBasePointSet = true;
        Log.d(TAG, "Base point set manually to = " + basePoint);
    }
    public void resetData() {
        scanCounter = 0;
        scanValues.clear();
        scanReady = false;
        basePoint = 0.0f;
        currentAverage = 0.0f;
        isBasePointSet = false;
        Log.d(TAG, "DataProcessor reset to default values.");
    }
}
