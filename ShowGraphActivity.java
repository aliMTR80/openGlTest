package com.novinsadr.graph.activityes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.novinsadr.graph.R;
import com.novinsadr.graph.adapters.BottomMenuSurfaceAdapter;
import com.novinsadr.graph.manager.BluetoothSerialManager;
import com.novinsadr.graph.manager.BottomMenuSurfaceItem;
import com.novinsadr.graph.renderer.DataProcessor;
import com.novinsadr.graph.renderer.MyGLRenderer;

import java.util.ArrayList;
import java.util.List;

import io.github.inflationx.viewpump.ViewPumpContextWrapper;

public class ShowGraphActivity extends AppCompatActivity implements BottomMenuSurfaceAdapter.OnItemClickListener, BluetoothSerialManager.OnDataReceivedListener {

    MyGLRenderer renderer;
    GLSurfaceView surfaceView;
    Button toggleModeButton; // Button for switching 2D/3D mode
    private boolean isFirstCall = true;
    private float previousX;
    private float previousY;
    private boolean isPlusModeActive = false; // اضافه کردن این متغیر
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private RecyclerView recyclerView;
    private BottomMenuSurfaceAdapter adapter;
    private List<BottomMenuSurfaceItem> items;
    private boolean isGraphBuilding = false;
    private final List<float[]> allGraphData = new ArrayList<>();

    private float basePoint = 0f;

    private int xGridSize = 0;
    private float[] zValuesArray = new float[10000]; // Initialize zValuesArray with a fixed size
    private int dataIndex = 0;
    private int currentXGrid = 0;
    private int currentYGrid = 0;
    private static final String TAG = "MainActivity";
    private boolean isZooming = false;
    private float lastZoomDistance = 0f;
    private boolean isZigzag;
    private String xValue;
    private DataProcessor dataProcessor;
    private DataProcessor.Mode currentMode;
    private BluetoothSerialManager bluetoothManager;
    private TextView txtZPosition;
    private SeekBar sensitivitySeekBar;

    @SuppressLint({"ClickableViewAccessibility", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_graph);

        surfaceView = findViewById(R.id.gl_surface_view);
        surfaceView.setEGLContextClientVersion(2);
        txtZPosition = findViewById(R.id.txt_z_position);
        renderer = new MyGLRenderer();
        surfaceView.setRenderer(renderer);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        xValue = getIntent().getStringExtra("xValue");
        isZigzag = getIntent().getBooleanExtra("isZigzag", false);
        recyclerView = findViewById(R.id.recyclerView);
        if (getIntent().getBooleanExtra(String.valueOf(MyGLRenderer.GraphType.SURFACE_3D), false)) {

            renderer.setGraphType(MyGLRenderer.GraphType.SURFACE_3D);
            renderer.scan3DMode(isZigzag);
            currentMode = DataProcessor.Mode.SCAN;
            recyclerView.setVisibility(View.VISIBLE);

        } else if (getIntent().getBooleanExtra(String.valueOf(MyGLRenderer.GraphType.POINT_MODE), false)) {
            isZigzag = getIntent().getBooleanExtra("isZigzag", false);
            renderer.setGraphType(MyGLRenderer.GraphType.POINT_MODE);
            currentMode = DataProcessor.Mode.POINT_LOCATOR;
            recyclerView.setVisibility(View.GONE);
        } else if (getIntent().getBooleanExtra(String.valueOf(MyGLRenderer.GraphType.LINE_GRAPH), false)) {
            isZigzag = getIntent().getBooleanExtra("isZigzag", false);
            renderer.setGraphType(MyGLRenderer.GraphType.LINE_GRAPH);
            currentMode = DataProcessor.Mode.MAGNETOMETER;
            recyclerView.setVisibility(View.GONE);
        }

        mGestureDetector = new GestureDetector(this, new GestureListener());
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        items = new ArrayList<>();
        items.add(new BottomMenuSurfaceItem(R.drawable.deoloyed, "حالت سه بعدی", getResources().getColor(R.color.white)));
        items.add(new BottomMenuSurfaceItem(R.drawable.ic_launcher_foreground, "مشبک", getResources().getColor(R.color.white)));
        items.add(new BottomMenuSurfaceItem(R.drawable.ic_launcher_foreground, "حذف قرمز", getResources().getColor(R.color.white)));
        items.add(new BottomMenuSurfaceItem(R.drawable.ic_launcher_foreground, "حذف سبز", getResources().getColor(R.color.white)));
        items.add(new BottomMenuSurfaceItem(R.drawable.ic_launcher_foreground, "حذف آبی", getResources().getColor(R.color.white)));
        items.add(new BottomMenuSurfaceItem(R.drawable.ic_launcher_foreground, "توقف اسکن", getResources().getColor(R.color.white)));
        items.add(new BottomMenuSurfaceItem(R.drawable.ic_launcher_foreground, "افزایش قرمز", getResources().getColor(R.color.white)));
        items.add(new BottomMenuSurfaceItem(R.drawable.ic_launcher_foreground, "کاهش قرمز", getResources().getColor(R.color.white)));
        items.add(new BottomMenuSurfaceItem(R.drawable.ic_launcher_foreground, "مختصات یاب", getResources().getColor(R.color.white)));
        items.add(new BottomMenuSurfaceItem(R.drawable.ic_launcher_foreground, "عمق", getResources().getColor(R.color.white)));
        adapter = new BottomMenuSurfaceAdapter(items, this);
        recyclerView.setAdapter(adapter);

        surfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isPlusModeActive) {
                    float touchX = event.getX();
                    float touchY = event.getY();

                    // فقط اگر لمس روی شیء باشد پردازش می‌کنیم
                    if (renderer.isTouchOnSurface(touchX, touchY)) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN ||
                                event.getAction() == MotionEvent.ACTION_MOVE) {
                            surfaceView.queueEvent(new Runnable() {
                                @Override
                                public void run() {
                                    renderer.handleTouchInput(touchX, touchY);
                                    surfaceView.requestRender();
                                }
                            });
                            return true;
                        }
                    }
                    return true; // برای جلوگیری از پردازش لمس‌های خارج از شیء
                } else { // اگر حالت نشانه گر + غیر فعال است (منطق قبلی برای زوم و چرخش)
                    mGestureDetector.onTouchEvent(event); // Pass event to gesture detector

                    int action = event.getActionMasked();

                    switch (action) {
                        case MotionEvent.ACTION_DOWN:
                            isZooming = false; // Reset zoom flag on new touch
                            if (event.getPointerCount() == 1) {
                                previousX = event.getX();
                                previousY = event.getY();
                            } else if (event.getPointerCount() == 2) {
                                isZooming = true;
                                lastZoomDistance = calculateDistance(event);
                            }
                            return true;
                        case MotionEvent.ACTION_POINTER_DOWN:
                            if (event.getPointerCount() == 2) {
                                isZooming = true;
                                lastZoomDistance = calculateDistance(event);
                            }
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            if (event.getPointerCount() == 1 && !isZooming) {
                                float deltaX = event.getX() - previousX;
                                float deltaY = event.getY() - previousY;
                                renderer.onTouchEvent(deltaX, deltaY, false);
                                previousX = event.getX();
                                previousY = event.getY();
                                surfaceView.requestRender();
                            } else if (event.getPointerCount() == 2) {
                                isZooming = true;
                                float newZoomDistance = calculateDistance(event);
                                float zoomFactor = newZoomDistance - lastZoomDistance; // Adjust zoom speed
                                renderer.onTouchEvent(0, zoomFactor, true); // deltaY is used for zoomFactor
                                lastZoomDistance = newZoomDistance;
                                surfaceView.requestRender();
                            }
                            return true;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_POINTER_UP:
                            if (event.getPointerCount() <= 1) {
                                isZooming = false;
                            }
                            return true;
                    }
                    return false;
                }
            }
        });

        renderer.setOnZValueCalculatedListener(new MyGLRenderer.OnZValueCalculatedListener() {
            @Override
            public void onZValueCalculated(float zValue) {
                runOnUiThread(() -> {
                    float zTrue = zValue * dataProcessor.DATA_MAX / 10;
                    txtZPosition.setText(String.valueOf(zTrue));
                });
            }
        });
        startGraphBuilding();

    }

    private float calculateDistance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void startGraphBuilding() {
        String xValueStr = xValue;

        try {
            xGridSize = Integer.parseInt(xValueStr) + 1;
            if (xGridSize < 0) {
                Toast.makeText(this, "اندازه محور X باید بیشتر از 1 باشد.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "فرمت داده‌های ورودی نامعتبر است.", Toast.LENGTH_SHORT).show();
            return;
        }

        allGraphData.clear();
        dataIndex = 0;
        currentXGrid = 1;
        currentYGrid = 0;
        isFirstCall = true; // Reset isFirstCall for new graph

        // Calculate scanThreshold based on xValue
        int x = Integer.parseInt(xValueStr);
        int scanThreshold = x + 17;

        // Initialize DataProcessor based on the current mode and scanThreshold (xGridSize)
        dataProcessor = new DataProcessor(currentMode, scanThreshold);

        renderer.setGraphBuilt(false);
        renderer.setDataPoints(new ArrayList<>()); // send empty list initially and call generateSurfaceVerticesFromData
        renderer.setGridSizeX(xGridSize);
        renderer.setGridSizeY(xGridSize);
        renderer.setBasePointValue(basePoint);
        renderer.generateSurfaceVerticesFromData(); // Call generateSurfaceVerticesFromData for initial build
        surfaceView.requestRender();

        isGraphBuilding = true;

        bluetoothManager = new BluetoothSerialManager();
        bluetoothManager.setOnDataReceivedListener(this);
        bluetoothManager.checkBluetoothPermissions(this);
    }


    private void updateGraphData(float zValue) {
        float processedZ = dataProcessor.processData(zValue);
        basePoint = dataProcessor.getBasePoint();
        renderer.setBasePointValue(basePoint);
        if (renderer.getCurrentGraphType() == MyGLRenderer.GraphType.SURFACE_3D) {
            renderer.onSurfaceChanged(null, surfaceView.getWidth(), surfaceView.getHeight());
        }
       if (DataProcessor.scanReady) {
           if (isFirstCall) {
               isFirstCall = false;

               // متغیرهای موقت برای نقاط صفر
               int tempX = 0;
               int tempY = 0;

               // اضافه کردن نقاط صفر برای پایه نمودار
               for (int i = 0; i < xGridSize + 1; i++) {
                   float x = tempX * renderer.gridSpacingX;
                   float y = tempY * renderer.gridSpacingY;

                   // اضافه کردن نقطه صفر به عنوان پایه
                   renderer.addDataPoint(new float[]{x, y, basePoint});

                   // به‌روزرسانی مختصات برای نقطه بعدی
                   tempY++;
                   if (tempY >= xGridSize) {
                       tempY = 0;
                       tempX++;
                   }
               }

               // تنظیم موقعیت برای شروع داده‌های اصلی
               currentXGrid = 1;
               currentYGrid = 0;

           } else {
               Log.d(TAG, String.format("Raw zValue: %.2f, ProcessedZ: %.2f, BasePoint: %.2f",
                       zValue, processedZ, basePoint));
               float x = currentXGrid * renderer.gridSpacingX;
               float y = currentYGrid * renderer.gridSpacingY;

               renderer.addDataPoint(new float[]{x, y, processedZ});
               Log.i(TAG, String.format("Added point - X: %.2f, Y: %.2f, Z: %.2f, Grid: %d,%d",
                       x, y, processedZ, currentXGrid, currentYGrid));

               // به‌روزرسانی موقعیت گرید
               currentYGrid++;
               if (currentYGrid >= xGridSize) {
                   currentYGrid = 0;
                   currentXGrid++;
               }

           }
       }
    }

    @Override
    public void onItemClick(int position) {
        switch (position) {
            case 0:
                renderer.toggleMode();
                BottomMenuSurfaceItem item = items.get(position);
                if (renderer.is2DMode()) {
                    item.setTitle("حالت سه بعدی"); // تغییر نام
                    item.setIcon(R.drawable.deoloyed); // تغییر آیکون
                    item.setBackgroundColor(getResources().getColor(R.color.white));
                } else {
                    item.setTitle("حالت دو بعدی"); // تغییر نام
                    item.setIcon(R.drawable.ic_launcher_foreground); // تغییر آیکون
                    item.setBackgroundColor(getResources().getColor(R.color.black));
                }
                surfaceView.requestRender();
                break;
            case 1:
                if (renderer.isGridVisible) {
                    renderer.setGridVisibility(false);
                } else {
                    renderer.setGridVisibility(true);
                }
                surfaceView.requestRender();
                break;
            case 2:
                if (renderer.RVisibility) {
                    renderer.showRGB(false, true, true);
                } else {
                    renderer.showRGB(true, true, true);
                }
                surfaceView.requestRender();
                break;
            case 3:
                if (renderer.GVisibility) {
                    renderer.showRGB(true, false, true);
                } else {
                    renderer.showRGB(true, true, true);
                }
                surfaceView.requestRender();
                break;
            case 4:
                if (renderer.BVisibility) {
                    renderer.showRGB(true, true, false);
                } else {
                    renderer.showRGB(true, true, true);
                }
                surfaceView.requestRender();
                break;
            case 5:
                renderer.setGraphBuilt(true);
                break;
            case 6:
                renderer.adjustBasePoint("+");
                surfaceView.requestRender();
                break;
            case 7:
                renderer.adjustBasePoint("-");
                surfaceView.requestRender();
                break;
            case 8:
                if (renderer.isPlusModeEnabled) {
                    renderer.isPlusModeEnabled = false;
                    isPlusModeActive = false;
                } else {
                    renderer.isPlusModeEnabled = true;
                    isPlusModeActive = true;
                }

        }
    }

    @Override
    public void onDataReceived(float value, byte[] rawPacket) {
        Log.i(TAG, "onDataReceived: " + DataProcessor.scanReady);

        updateGraphData(value);
        surfaceView.requestRender();

    }

    class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // Handle double tap if needed, for now just for example
            Toast.makeText(ShowGraphActivity.this, "Double Tap", Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    //متد پاکسازی متغییر ها و ازادسازی رشته ها و ....
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called in ShowGraphActivity...");

        // قطع اتصال بلوتوث
        if (bluetoothManager != null) {
            bluetoothManager.disconnect();
            bluetoothManager = null;
        }

        // پاکسازی آرایه zValuesArray
        if (zValuesArray != null) {
            zValuesArray = null;
        }

        // متوقف کردن پردازش داده‌ها
        isGraphBuilding = false;

        if (dataProcessor != null) {
            dataProcessor.resetData();
        }
        // آزاد سازی منابع مربوط به DataProcessor
        if (dataProcessor != null) {
            dataProcessor = null;
        }
        // فراخوانی متد releaseResources در MyGLRenderer
        if (renderer != null) {
            renderer.releaseResources();
            renderer = null;
        }
    }

    // پاس دادن نتیجه مجوز به BluetoothManager
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        bluetoothManager.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    // پاس دادن نتیجه فعال سازی بلوتوث به BluetoothManager
    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        bluetoothManager.onActivityResult(requestCode, resultCode, this);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }
}