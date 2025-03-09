package com.novinsadr.myapplication;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    // OpenGL related
    private GLSurfaceView mGLSurfaceView;
    private Graph3DRenderer mRenderer;

    // Touch handling
    private float mPreviousX;
    private float mPreviousY;
    private static final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private static final float ZOOM_SCALE_FACTOR = 0.2f;
    private static final int TOUCH_MODE_ROTATE = 1;
    private static final int TOUCH_MODE_ZOOM = 2;
    private int mTouchMode = TOUCH_MODE_ROTATE;

    // Data simulation
    private static final int UPDATE_INTERVAL = 300; // milliseconds
    private static final int MAX_DATA_POINTS = 100;
    private Handler mHandler = new Handler();
    private Random mRandom = new Random();
    private AtomicBoolean isSimulating = new AtomicBoolean(false);
    private float[] dataPoints = new float[MAX_DATA_POINTS];
    private int currentDataIndex = 0;

    // Data processing
    private DataProcessor mDataProcessor;
    private float baseValue = 0f;
    private boolean isBaseSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize data processor
        mDataProcessor = new DataProcessor(DataProcessor.Mode.MAGNETOMETER);

        // Create GLSurfaceView
        mGLSurfaceView = new GLSurfaceView(this);
        mRenderer = new Graph3DRenderer();
        mGLSurfaceView.setRenderer(mRenderer);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        // Setup touch handling
        setupTouchListener();

        setContentView(mGLSurfaceView);

        // Start data simulation
        startDataSimulation();
    }

    private void setupTouchListener() {
        mGLSurfaceView.setOnTouchListener((v, event) -> {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    float dx = x - mPreviousX;
                    float dy = y - mPreviousY;

                    if (event.getPointerCount() == 2) {
                        handleZoom(dy);
                    } else {
                        handleRotation(x, y, dx, dy);
                    }
                    break;
            }

            mPreviousX = x;
            mPreviousY = y;
            mGLSurfaceView.requestRender();
            return true;
        });
    }

    private void handleZoom(float dy) {
        mTouchMode = TOUCH_MODE_ZOOM;
        float zoom = dy * ZOOM_SCALE_FACTOR;
        mRenderer.zoom(zoom);
    }

    private void handleRotation(float x, float y, float dx, float dy) {
        mTouchMode = TOUCH_MODE_ROTATE;

        // Adjust rotation based on screen quadrants
        if (y > mGLSurfaceView.getHeight() / 2) {
            dx *= -1;
        }
        if (x < mGLSurfaceView.getWidth() / 2) {
            dy *= -1;
        }

        mRenderer.setAngle(
                mRenderer.getAngleX() + (dy * TOUCH_SCALE_FACTOR),
                mRenderer.getAngleY() + (dx * TOUCH_SCALE_FACTOR),
                mRenderer.getAngleZ()
        );
    }

    private void startDataSimulation() {
        isSimulating.set(true);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!isSimulating.get()) return;

                // Generate random sensor data
                float rawValue = mRandom.nextFloat() * 1080;

                // Process the data through DataProcessor
                float processedValue = mDataProcessor.processData(rawValue);

                // Update the data array
                updateDataPoints(processedValue);

                // Update the renderer
                updateRenderer();

                // Schedule next update
                mHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        });
    }

    private void updateDataPoints(float newValue) {
        // Shift existing data points
        System.arraycopy(dataPoints, 1, dataPoints, 0, dataPoints.length - 1);

        // Add new value at the end
        dataPoints[dataPoints.length - 1] = newValue;

        // Update current index
        currentDataIndex = Math.min(currentDataIndex + 1, MAX_DATA_POINTS - 1);
    }

    private void updateRenderer() {
        int width = 50;  // تعداد نقاط در محور X
        int height = 50; // تعداد نقاط در محور Z
        float[] data = new float[width * height];

        // ایجاد داده‌های تست به صورت سطح موجی
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                float xf = x / (float)(width - 1) * 4 * (float)Math.PI;
                float zf = z / (float)(height - 1) * 4 * (float)Math.PI;

                // ترکیب چند موج سینوسی برای ایجاد سطح پیچیده‌تر
                data[z * width + x] =
                        (float)(Math.sin(xf) * Math.cos(zf) * 0.5f +
                                Math.sin(xf * 2) * Math.cos(zf * 2) * 0.25f);

                // نرمال‌سازی به محدوده [-1, 1]
                data[z * width + x] = data[z * width + x] * 0.5f;
            }
        }

        // به‌روزرسانی رندرر با داده‌های جدید
        mRenderer.setData(data, width, height);
        mGLSurfaceView.requestRender();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Reset View");
        menu.add(0, 2, 0, "Toggle Grid");
        menu.add(0, 3, 0, "Toggle Axes");
        menu.add(0, 4, 0, "Change View Mode");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                mRenderer.resetTransformation();
                break;
            case 2:
                mRenderer.toggleGrid();
                break;
            case 3:
                mRenderer.toggleAxes();
                break;
            case 4:
                cycleViewMode();
                break;
        }
        mGLSurfaceView.requestRender();
        return true;
    }

    private void cycleViewMode() {
        byte currentMode = mRenderer.getCurrentView();
        byte newMode = (byte)((currentMode + 1) % 4);
        mRenderer.setViewMode(newMode);

        String viewName;
        switch (newMode) {
            case Graph3DRenderer.VIEW_TOP:
                viewName = "Top View";
                break;
            case Graph3DRenderer.VIEW_SIDE:
                viewName = "Side View";
                break;
            case Graph3DRenderer.VIEW_PERSPECTIVE:
                viewName = "Perspective View";
                break;
            default:
                viewName = "Custom View";
                break;
        }
        Toast.makeText(this, viewName, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isSimulating.set(false);
        mGLSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
        startDataSimulation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isSimulating.set(false);
        mHandler.removeCallbacksAndMessages(null);
    }
}