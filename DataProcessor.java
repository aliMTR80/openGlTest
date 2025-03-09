package com.novinsadr.myapplication;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class DataProcessor {
    private static final String TAG = "DataProcessor";

    // Operating modes
    public enum Mode {
        SCAN,           // For scanning operations
        MAGNETOMETER,   // For magnetometer readings
        POINT_LOCATOR  // For point location
    }

    // Data processing constants
    private static final float DATA_MIN = 0f;
    private static final float DATA_MAX = 32768f;
    private static final float NORMALIZED_MIN = 0.0f;
    private static final float NORMALIZED_MAX = 1000.0f;

    // Scan related parameters
    private static final int MAX_SCAN_BUFFER = 100;
    private static final float DECAY_FACTOR = 0.9f;
    private static final float MIN_THRESHOLD = 5.0f;

    // Instance variables
    private Mode mode;
    private int scanThreshold;
    private int scanCounter;
    private List<Float> scanValues;
    private float basePoint;
    private float currentAverage;
    private boolean isBasePointSet;
    private float lastProcessedValue;
    private float smoothingFactor;
    private boolean autoBaseline;

    // Signal processing buffers
    private List<Float> signalBuffer;
    private List<Float> filteredBuffer;

    // Statistics tracking
    private float minValue = Float.MAX_VALUE;
    private float maxValue = Float.MIN_VALUE;
    private float meanValue = 0;
    private int sampleCount = 0;

    /**
     * Constructor with mode and threshold
     */
    public DataProcessor(Mode mode, int scanThreshold) {
        init(mode);
        this.scanThreshold = scanThreshold;
    }

    /**
     * Constructor with mode only
     */
    public DataProcessor(Mode mode) {
        init(mode);
        this.scanThreshold = MAX_SCAN_BUFFER;
    }

    /**
     * Initialize processor
     */
    private void init(Mode mode) {
        this.mode = mode;
        this.scanCounter = 0;
        this.scanValues = new ArrayList<>();
        this.isBasePointSet = false;
        this.basePoint = 0;
        this.currentAverage = 0;
        this.smoothingFactor = 0.1f;
        this.autoBaseline = true;
        this.signalBuffer = new ArrayList<>();
        this.filteredBuffer = new ArrayList<>();
        resetStatistics();
    }

    /**
     * Process incoming data based on current mode
     */
    public float processData(float value) {
        updateStatistics(value);

        switch (mode) {
            case SCAN:
                return processScanData(value);
            case MAGNETOMETER:
            case POINT_LOCATOR:
                return processMagnetometerData(value);
            default:
                return normalizeData(value);
        }
    }

    /**
     * Process data in scan mode
     */
    private float processScanData(float value) {
        scanValues.add(value);
        scanCounter++;

        if (scanCounter >= scanThreshold) {
            // Calculate moving average
            float sum = 0;
            for (float v : scanValues) {
                sum += v;
            }
            currentAverage = sum / scanThreshold;

            // Set baseline if needed
            if (!isBasePointSet || autoBaseline) {
                basePoint = normalizeToNewRange(currentAverage);
                isBasePointSet = true;
            }

            // Clear buffer for next scan
            scanValues.clear();
            scanCounter = 0;

            return processMagnetometerData(value);
        }

        return isBasePointSet ? processMagnetometerData(value) : NORMALIZED_MIN;
    }

    /**
     * Process magnetometer data
     */
    private float processMagnetometerData(float value) {
        if (!isBasePointSet) {
            basePoint = normalizeToNewRange(value);
            isBasePointSet = true;
            lastProcessedValue = value;
            return normalizeData(value);
        }

        // Apply exponential smoothing
        float smoothedValue = (smoothingFactor * value) +
                ((1 - smoothingFactor) * lastProcessedValue);
        lastProcessedValue = smoothedValue;

        // Apply digital filtering
        signalBuffer.add(smoothedValue);
        if (signalBuffer.size() > MAX_SCAN_BUFFER) {
            signalBuffer.remove(0);
        }

        // Apply median filter for noise reduction
        float filteredValue = applyMedianFilter(signalBuffer);
        filteredBuffer.add(filteredValue);
        if (filteredBuffer.size() > MAX_SCAN_BUFFER) {
            filteredBuffer.remove(0);
        }

        return normalizeData(filteredValue);
    }

    /**
     * Normalize data to output range
     */
    private float normalizeData(float value) {
        float normalizedValue = normalizeToNewRange(value);

        // Apply thresholding
        if (Math.abs(normalizedValue) < MIN_THRESHOLD) {
            normalizedValue = normalizedValue >= 0 ? MIN_THRESHOLD : -MIN_THRESHOLD;
        }

        // Limit to valid range
        normalizedValue = Math.max(NORMALIZED_MIN, Math.min(NORMALIZED_MAX, normalizedValue));

        Log.d(TAG, String.format("Original: %.2f, Base: %.2f, Normalized: %.2f",
                value, basePoint, normalizedValue));

        return normalizedValue;
    }

    /**
     * Convert value to new range
     */
    private float normalizeToNewRange(float value) {
        return (value * NORMALIZED_MAX) / DATA_MAX;
    }

    /**
     * Apply median filter to reduce noise
     */
    private float applyMedianFilter(List<Float> values) {
        if (values.size() < 3) return values.get(values.size() - 1);

        ArrayList<Float> sorted = new ArrayList<>(values);
        java.util.Collections.sort(sorted);
        return sorted.get(sorted.size() / 2);
    }

    /**
     * Update statistical measurements
     */
    private void updateStatistics(float value) {
        minValue = Math.min(minValue, value);
        maxValue = Math.max(maxValue, value);
        sampleCount++;
        meanValue = meanValue + (value - meanValue) / sampleCount;
    }

    /**
     * Reset statistics
     */
    private void resetStatistics() {
        minValue = Float.MAX_VALUE;
        maxValue = Float.MIN_VALUE;
        meanValue = 0;
        sampleCount = 0;
    }

    // Getters and Setters

    public void setBasePoint(float value) {
        this.currentAverage = value;
        this.basePoint = normalizeToNewRange(value);
        this.isBasePointSet = true;
        Log.d(TAG, "Base point set manually to = " + basePoint);
    }

    public float getBasePoint() {
        return basePoint;
    }

    public void setSmoothingFactor(float factor) {
        this.smoothingFactor = Math.max(0.0f, Math.min(1.0f, factor));
    }

    public void setAutoBaseline(boolean auto) {
        this.autoBaseline = auto;
    }

    public float getMinValue() {
        return minValue;
    }

    public float getMaxValue() {
        return maxValue;
    }

    public float getMeanValue() {
        return meanValue;
    }

    public void resetData() {
        scanCounter = 0;
        scanValues.clear();
        signalBuffer.clear();
        filteredBuffer.clear();
        basePoint = 0.0f;
        currentAverage = 0.0f;
        isBasePointSet = false;
        lastProcessedValue = 0.0f;
        resetStatistics();
        Log.d(TAG, "DataProcessor reset to default values.");
    }
}