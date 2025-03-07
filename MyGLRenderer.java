package com.novinsadr.graph.renderer;

import static android.opengl.GLES20.GL_LINE_STRIP;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLRenderer implements GLSurfaceView.Renderer {

    private float midScreenY;
    // متغیرهای مربوط به مثلث
// متغیرهای مربوط به مثلث
    private FloatBuffer triangleVertexBuffer;
    private float triangleBaseWidth = 0.5f;
    private float triangleHeight = 0.5f;
    private float minXData = Float.MAX_VALUE;
    private float maxXData = Float.MIN_VALUE;
    private float minYData = Float.MAX_VALUE;
    private float maxYData = Float.MIN_VALUE;
    private static final String TAG = "SurfaceRenderer";
    private FloatBuffer surfaceVertexBuffer;
    private ShortBuffer surfaceIndexBuffer;
    private int surfaceProgram;
    private int surfacePositionHandle;
    private int surfaceMvpMatrixHandle;
    private FloatBuffer lineZValueBuffer;
    private int surfaceBasePointHandle;
    private int numIndices;
    private FloatBuffer lineVertexBuffer;
    private int lineVertexCount = 0;
    private FloatBuffer axisVertexBuffer;
    public boolean isPlusModeEnabled = false;
    private int axisProgram;
    private int axisPositionHandle;
    private int axisColorHandle;
    private int axisMvpMatrixHandle;
    private int axisVertexCount = 0;
    private float xOffset2D = 0f;
    private float yOffset2D = 0f;
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private float zoom = -15f;
    private final float fixedCameraZ_TopDown = 10f;
    private final float cameraY_3D = 0f;
    private int gridSizeX = 0;
    private int gridSizeY = 0;
    public float gridSpacingX = 1f;
    public float gridSpacingY = 1f;
    private float minZ = 0;
    private float maxZ = 10f;
    private float minY = 0f;
    private float minX = 0f;
    private float maxX = 0f;
    private float maxY = 0f;
    private float centerX = 0f;
    private float centerY = 0f;
    private float centerZ = 0f;
    private float basePointValue = 0f;
    private List<float[]> dataPoints;
    public boolean isGraphBuilt = false;
    private boolean is2DMode = true;
    private float xRotation = 0f;
    private float yRotation = 0f;
    private float zoomFactor = 1.0f;
    public boolean isGridVisible = false;
    private float zScaleFactor = 1f;
    private boolean isZigzag = false;
    public Boolean RVisibility = true; // مقادیر اولیه برای متغیرهای visibility - قابل تغییر در اکتیویتی
    public Boolean GVisibility = true;
    public Boolean BVisibility = true;
    private int rVisibilityHandle;
    private int gVisibilityHandle;
    private int bVisibilityHandle;
    public boolean multiColors = false;
    private int fragmentShader;
    private int surfaceWidth;
    private int surfaceHeight;
    private float[] plusPosition = {0.0f, 0.0f, 0.0f}; // موقعیت نشانه گر +
    private float plusSize = 0.5f; // اندازه نشانه گر +
    private final float[] plusColor = {1.0f, 1.0f, 1.0f, 1.0f}; // رنگ نشانه گر سفید

    private FloatBuffer plusVertexBuffer;
    private int plusProgram;
    private int plusPositionHandle;
    private int plusColorHandle;
    private int plusMvpMatrixHandle;
    private int plusTranslationHandle;


    public MyGLRenderer() {
        gridSpacingX = 1f;
        gridSpacingY = 1f;
        isGridVisible = false;
        zScaleFactor = 1.0f;
        dataPoints = new ArrayList<>();
        is2DMode = true;
    }

    public void setDataPoints(List<float[]> dataPoints) {
        this.dataPoints = dataPoints;
        generateSurfaceVerticesFromData();
        generateAxisVertices();
    }

    public void setPlusModeVisibility(boolean visibility) {
        this.isPlusModeEnabled = visibility;
    }

    private GraphType currentGraphType = GraphType.SURFACE_3D;

    public enum GraphType {
        SURFACE_3D,
        LINE_GRAPH,
        POINT_MODE
        }

    public void scan3DMode(boolean mode) {
        this.isZigzag = mode;
    }

    public void setMultiColors(boolean visibility) {
        this.multiColors = visibility;
    }

    public void showRGB(boolean R, boolean G, boolean B) {
        this.RVisibility = R;
        this.GVisibility = G;
        this.BVisibility = B;
    }

    public void setGridSizeX(int gridSizeX) {
        this.gridSizeX = gridSizeX;
    }

    public void setGridSizeY(int gridSizeY) {
        this.gridSizeY = gridSizeY;
    }

    public void setBasePointValue(float basePointValue) {
        this.basePointValue = basePointValue;
    }

    public void setGraphBuilt(boolean built) {
        isGraphBuilt = built;
        if (isGraphBuilt) {
            setup2DMode();
        } else {
            is2DMode = false;
        }
    }

    public boolean is2DMode() {
        return is2DMode;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);



        initSurfaceShaders();
        initAxisShaders();
        initPlusShaders();
        generateTriangleVertices();
        setTriangleSize(1.0f, 1.0f);

        generateAxisVertices();
        generateSurfaceVerticesFromData();
        generateLineGraphVertices();
        generatePlusVertices();
        setup2DMode();

        Log.d(TAG, "Initial mode setup in onSurfaceCreated completed.");
    }

    private void initSurfaceShaders() {
        String vertexShaderCode =
                "precision highp float;" +
                        "uniform mat4 u_MVPMatrix;" +
                        "attribute vec4 vPosition;" +
                        "varying highp float vColorFactor;" +
                        "varying highp float vXPosition;" +  // اضافه کردن متغیر برای موقعیت X
                        "uniform float u_BasePoint;" +  // نقطه مبنا (مثلاً 0.23)
                        "uniform float u_minZ;" +
                        "uniform float u_maxZ;" +
                        "void main() {" +
                        "  float normalizedZ = (vPosition.z - u_minZ) / (u_maxZ - u_minZ);" +
                        "  vColorFactor = normalizedZ;" +
                        "  vXPosition = vPosition.x;" +  // انتقال موقعیت X
                        "  gl_Position = u_MVPMatrix * vPosition;" +
                        "}";

        String fragmentShaderCodeMultiColors =
                "precision highp float;" +
                        "varying highp float vColorFactor;" +
                        "varying highp float vXPosition;" +
                        "uniform float u_BasePoint;" +
                        "uniform float u_minZ;" +
                        "uniform float u_maxZ;" +
                        "uniform bool RVisibility;" +  // یونیفرم برای کنترل نمایش رنگ‌های قرمز، نارنجی و زرد
                        "uniform bool GVisibility;" +  // یونیفرم برای کنترل نمایش رنگ سبز
                        "uniform bool BVisibility;" +  // یونیفرم برای کنترل نمایش رنگ‌های آبی

                        "void main() {" +
                        // تعریف رنگ‌ها
                        "    vec4 green      = vec4(0.0, 0.902, 0.0, 1.0);" +
                        "    vec4 yellow     = vec4(1.0, 0.902, 0.0, 1.0);" +
                        "    vec4 orange     = vec4(1.0, 0.651, 0.0, 1.0);" +
                        "    vec4 red        = vec4(0.902, 0.486, 0.0, 1.0);" +
                        "    vec4 lightBlue  = vec4(0.0, 0.761, 0.949, 1.0);" +
                        "    vec4 darkBlue   = vec4(0.0, 0.0, 0.902, 1.0);" +

                        // نرمال‌سازی basePoint
                        "    float normalizedBase = (u_BasePoint - u_minZ) / (u_maxZ - u_minZ);" +
                        "    vec4 finalColor;" +
                        "    float relativePos = vColorFactor - normalizedBase;" +

                        // تقسیم‌بندی جدید محدوده‌ها و اعمال Visibility
                        "    if (relativePos < 0.0) {" + // محدوده‌ی رنگ‌های آبی و سبز مایل به آبی
                        "        if (!BVisibility) discard;" + // حذف کامل رنگ‌های آبی در صورت false بودن BVisibility
                        "        if (relativePos < -0.15) {" +
                        "            finalColor = darkBlue;" +
                        "        } else if (relativePos < -0.05) {" +
                        "            float t1 = smoothstep(-0.15, -0.05, relativePos);" +
                        "            finalColor = mix(darkBlue, lightBlue, t1);" +
                        "        } else {" +
                        "            float t2 = smoothstep(-0.05, 0.0, relativePos);" +
                        "            finalColor = mix(lightBlue, green, t2);" +
                        "        }" +
                        "    } else {" + // محدوده‌ی رنگ‌های سبز و گرم (زرد، نارنجی، قرمز)
                        "        if (relativePos <= 0.04) {" + // کاهش محدوده رنگ سبز
                        "            if (!GVisibility) discard;" + // حذف کامل رنگ سبز در صورت false بودن GVisibility
                        "            finalColor = green;" +
                        "        } else {" +
                        "            if (!RVisibility) discard;" +
                        "            float t1 = smoothstep(0.04, 0.1, relativePos);" + // افزایش محدوده رنگ زرد
                        "            float t2 = smoothstep(0.1, 0.2, relativePos);" + // افزایش محدوده رنگ نارنجی
                        "            float t3 = smoothstep(0.2, 0.3, relativePos);" + // افزایش محدوده رنگ قرمز

                        "            if (relativePos <= 0.1) {" +
                        "                finalColor = mix(green, yellow, t1);" +
                        "            } else if (relativePos < 0.2) {" +
                        "                finalColor = mix(yellow, orange, t2);" +
                        "            } else {" +
                        "                finalColor = mix(orange, red, t3);" +
                        "            }" +
                        "        }" +
                        "    }" +

                        // اعمال سایه‌زنی نرم
                        "    float shadow = smoothstep(-1.0, 1.0, sin(vXPosition * 0.5)) * 0.4 + 0.6;" +
                        "    finalColor.rgb *= shadow;" +

                        "    gl_FragColor = finalColor;" +
                        "}";
        String fragmentShaderCode =
                "precision highp float;" +
                        "varying highp float vColorFactor;" +
                        "varying highp float vXPosition;" +
                        "uniform float u_BasePoint;" +
                        "uniform float u_minZ;" +
                        "uniform float u_maxZ;" +
                        "uniform bool RVisibility;" +  // یونیفرم برای کنترل نمایش رنگ نارنجی
                        "uniform bool GVisibility;" +  // یونیفرم برای کنترل نمایش رنگ سبز
                        "uniform bool BVisibility;" +  // یونیفرم برای کنترل نمایش رنگ آبی

                        "void main() {" +
                        // تعریف رنگ‌ها - فقط نارنجی، سبز و آبی
                        "    vec4 green      = vec4(0.0, 0.902, 0.0, 1.0);" + // سبز ثابت
                        "    vec4 orange     = vec4(1.0, 0.651, 0.0, 1.0);" + // نارنجی ثابت
                        "    vec4 blue       = vec4(0.0, 0.0, 0.902, 1.0);" + // آبی ثابت

                        // نرمال‌سازی basePoint
                        "    float normalizedBase = (u_BasePoint - u_minZ) / (u_maxZ - u_minZ);" +
                        "    vec4 finalColor;" +
                        "    float relativePos = vColorFactor - normalizedBase;" +

                        // تقسیم‌بندی محدوده‌ها - ساده شده برای 3 رنگ و اعمال Visibility
                        "    if (relativePos < 0.0) {" +
                        "       if(!BVisibility) discard; " + // حذف کامل رنگ‌های آبی در صورت false بودن BVisibility
                        // آبی از -∞ تا 0.0
                        "        float t1 = smoothstep(-0.03, 0.0, relativePos);" + // گذار نرم از آبی به سبز
                        "        finalColor = mix(blue, green, t1);" +
                        "    } else {" +
                        // محدوده رنگ‌های سبز و نارنجی
                        "        if (relativePos <= 0.2) { " + // محدوده سبز و گذار سبز به نارنجی (تغییر محدوده به 0.2 برای پوشش کامل گذار)
                        "           if(!GVisibility) discard; " + // حذف کامل رنگ سبز و گذار های آن در صورت false بودن GVisibility
                        "           float t2 = smoothstep(0.0, 0.03, relativePos);" +     // گذار نرم از سبز به نارنجی
                        "           finalColor = mix(green, orange, t2);" +
                        "        } else {" +
                        "           if(!RVisibility) discard; " + // حذف کامل رنگ‌های نارنجی در صورت false بودن RVisibility
                        "           finalColor = orange;" + // رنگ نارنجی برای relativePos > 0.2
                        "        }" +
                        "    }" +

                        // اعمال سایه‌زنی نرم
                        "    float shadow = smoothstep(-1.0, 1.0, sin(vXPosition * 0.5)) * 0.4 + 0.6;" +
                        "    finalColor.rgb *= shadow;" +

                        "    gl_FragColor = finalColor;" +
                        "}";

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        if (multiColors) {
            fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCodeMultiColors);
        } else {
            fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        }


        surfaceProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(surfaceProgram, vertexShader);
        GLES20.glAttachShader(surfaceProgram, fragmentShader);
        GLES20.glLinkProgram(surfaceProgram);

        final int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(surfaceProgram, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Error linking surface shader program: " + GLES20.glGetProgramInfoLog(surfaceProgram));
            GLES20.glDeleteProgram(surfaceProgram);
            surfaceProgram = 0;
        } else {
            GLES20.glUseProgram(surfaceProgram);
            surfacePositionHandle = GLES20.glGetAttribLocation(surfaceProgram, "vPosition");
            surfaceMvpMatrixHandle = GLES20.glGetUniformLocation(surfaceProgram, "u_MVPMatrix");


            if (surfacePositionHandle == -1 || surfaceMvpMatrixHandle == -1) {
                Log.e(TAG, "Error getting surface shader attribute or uniform locations.");
            }
        }
    }


    private void initAxisShaders() {
        String vertexShaderCode =
                "uniform mat4 u_MVPMatrix;" +
                        "attribute vec4 vPosition;" +
                        "void main() {" +
                        "  gl_Position = u_MVPMatrix * vPosition;" +
                        "}";

        String fragmentShaderCode =
                "precision mediump float;" +
                        "uniform vec4 u_Color;" +
                        "void main() {" +
                        "  gl_FragColor = u_Color;" +
                        "}";

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        axisProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(axisProgram, vertexShader);
        GLES20.glAttachShader(axisProgram, fragmentShader);
        GLES20.glLinkProgram(axisProgram);

        final int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(axisProgram, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Error linking axis shader program: " + GLES20.glGetProgramInfoLog(axisProgram));
            GLES20.glDeleteProgram(axisProgram);
            axisProgram = 0;
        } else {
            GLES20.glUseProgram(axisProgram);
            axisPositionHandle = GLES20.glGetAttribLocation(axisProgram, "vPosition");
            axisColorHandle = GLES20.glGetUniformLocation(axisProgram, "u_Color");
            axisMvpMatrixHandle = GLES20.glGetUniformLocation(axisProgram, "u_MVPMatrix");

            if (axisPositionHandle == -1 || axisColorHandle == -1 || axisMvpMatrixHandle == -1) {
                Log.e(TAG, "Error getting axis shader attribute or uniform locations.");
            }
        }
    }

    public void generateSurfaceVerticesFromData() {
        if (dataPoints == null || dataPoints.isEmpty()) {
            minX = -5f;
            maxX = 5f;
            minY = -5f;
            maxY = 5f;
            minZ = -1f;
            maxZ = 1f;

            Log.e(TAG, "No data points provided.");
            return;
        }

        Log.d(TAG, "generateSurfaceVerticesFromData: dataPoints size = " + dataPoints.size());

        // Calculate min/max values *FIRST*
        minZ = Float.MAX_VALUE;
        maxZ = -Float.MAX_VALUE;
        minY = Float.MAX_VALUE;
        minX = Float.MAX_VALUE;
        maxX = -Float.MAX_VALUE;
        maxY = -Float.MAX_VALUE;

        boolean hasValidData = false;

        for (float[] point : dataPoints) {
            if (point != null && point.length >= 3) {
                minZ = Math.min(minZ, point[2]);
                maxZ = Math.max(maxZ, point[2]);
                minY = Math.min(minY, point[1]);
                maxY = Math.max(maxY, point[1]);
                minX = Math.min(minX, point[0]);
                maxX = Math.max(maxX, point[0]);
                hasValidData = true;
            }
        }

        // اگر هیچ داده معتبری وجود نداشته باشد، از مقادیر پیش‌فرض استفاده کنید
        if (!hasValidData) {
            Log.w(TAG, "No valid data points found. Using default ranges.");
            minX = -5f;
            maxX = 5f;
            minY = -5f;
            maxY = 5f;
            minZ = -1f;
            maxZ = 1f;
            Log.i(TAG, "Using default Z range.");
        }

        Log.d(TAG, "generateSurfaceVerticesFromData: minX=" + minX + ", maxX=" + maxX + ", minY=" + minY + ", maxY=" + maxY + ", minZ=" + minZ + ", maxZ=" + maxZ);

        // محاسبه مرکز شیء - مرکز Z هم اکنون با مقیاس‌دهی Z تنظیم می‌شود
        centerX = (maxX + minX) / 2f;
        centerY = (maxY + minY) / 2f;
        centerZ = (maxZ + minZ) / 2f * zScaleFactor; // مرکز Z هم اکنون با مقیاس‌دهی Z تنظیم می‌شود
        Log.d(TAG, "Center Point: centerX=" + centerX + ", centerY=" + centerY + ", centerZ=" + centerZ);

        int totalDataPoints = dataPoints.size();
        if (gridSizeX <= 0) gridSizeX = 1;
        int numRows = (int) Math.ceil((double) (totalDataPoints / gridSizeX) + 1);
        int numSquaresX = gridSizeX - 1;
        int numSquaresY = (int) Math.ceil((double) totalDataPoints / gridSizeX);

        int totalVertices = numRows * gridSizeX;
        List<Short> indexList = new ArrayList<>();
        List<Float> verticesList = new ArrayList<>(); // استفاده از ArrayList برای رئوس

        float worldY;
        float worldX;
        for (int x_row = 0; x_row < numRows; x_row++) {
            for (int y_col = 0; y_col < gridSizeX; y_col++) {
                if (isZigzag) {
                    if (numRows % 2 != 0) {
                        worldY = maxY - y_col * gridSpacingY;
                    } else {
                        worldY = minY + y_col * gridSpacingY;
                    }
                    worldX = minX + x_row * gridSpacingX;
                } else {
                    worldX = minX + x_row * gridSpacingX;
                    worldY = minY + y_col * gridSpacingY;
                }
                float z;
                int dataPointIndex = x_row * gridSizeX + y_col;
                if (dataPointIndex < totalDataPoints) { // از totalDataPoints استفاده کنید
                    if (dataPoints.get(dataPointIndex) != null && dataPoints.get(dataPointIndex).length >= 3) {
                        float[] point = dataPoints.get(dataPointIndex);
                        z = point[2] * zScaleFactor; // مقدار z را از نقطه داده دریافت کنید

                        verticesList.add(worldX);
                        verticesList.add(worldY);
                        verticesList.add(z);
                    } else {
                        // اگر نقطه داده معتبر نیست، این راس را رد کنید
                        Log.w(TAG, "Data point missing for vertex at: x=" + worldX + ", y=" + worldY + ", skipping vertex");
                    }
                } else {
                    // اگر dataPointIndex از محدوده خارج است، این راس را رد کنید
                    Log.w(TAG, "Data point index out of bounds: x=" + worldX + ", y=" + worldY + ", skipping vertex");
                }
            }
        }

        // Generate indices for quads
        for (int row = 0; row < numSquaresY; row++) {
            for (int col = 0; col < numSquaresX; col++) {
                int p0Index = row * gridSizeX + col;
                int p1Index = row * gridSizeX + col + 1;
                int p2Index = (row + 1) * gridSizeX + col;
                int p3Index = (row + 1) * gridSizeX + col + 1;

                if (p0Index < totalDataPoints && p1Index < totalDataPoints &&
                        p2Index < totalDataPoints && p3Index < totalDataPoints &&
                        dataPoints.get(p0Index) != null && dataPoints.get(p1Index) != null &&
                        dataPoints.get(p2Index) != null && dataPoints.get(p3Index) != null) {

                    short p0 = (short) p0Index;
                    short p1 = (short) p1Index;
                    short p2 = (short) p2Index;
                    short p3 = (short) p3Index;

                    indexList.add(p0);
                    indexList.add(p1);
                    indexList.add(p3);
                    indexList.add(p2);
                } else {
                    Log.d(TAG, "Skipping index generation for square at row=" + row + ", col=" + col + " due to missing data at edges");
                }
            }
        }

        // Convert index list to array
        short[] indices = new short[indexList.size()];
        for (int i = 0; i < indexList.size(); i++) {
            indices[i] = indexList.get(i);
        }

        // Convert vertices list to array
        float[] vertices = new float[verticesList.size()];
        for (int i = 0; i < verticesList.size(); i++) {
            vertices[i] = verticesList.get(i);
        }

        // Create vertex buffer
        try {
            ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
            bb.order(ByteOrder.nativeOrder());
            surfaceVertexBuffer = bb.asFloatBuffer();
            surfaceVertexBuffer.put(vertices);
            surfaceVertexBuffer.position(0);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Out of memory creating vertex buffer: " + e.getMessage());
            surfaceVertexBuffer = null;
            return;
        }

        // Create index buffer
        try {
            ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
            ibb.order(ByteOrder.nativeOrder());
            surfaceIndexBuffer = ibb.asShortBuffer();
            try {
                surfaceIndexBuffer.put(indices);
            } catch (Exception e) {
                Log.i(TAG, "generateSurfaceVerticesFromData: buffer");
            }

            surfaceIndexBuffer.position(0);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Out of memory creating index buffer: " + e.getMessage());
            surfaceIndexBuffer = null;
            return;
        }

        // Store the number of indices for rendering
        numIndices = indices.length;

        Log.d(TAG, "generateSurfaceVerticesFromData: Generated " + totalVertices + " vertices and " + numIndices + " indices for surface.");
    }

    private void generateAxisVertices() {
        Log.d(TAG, "generateAxisVertices: minX=" + minX + ", maxX=" + maxX + ", minY=" + minY + ", maxY=" + maxY + ", minZ=" + minZ + ", maxZ=" + maxZ);

        // استفاده از minX، maxX، minY، maxY, minZ, maxZ برای محدوده محورها
        float startX = minX;
        float endX = maxX;
        float startY = minY;
        float endY = maxY;
        float startZ = minZ;
        float endZ = maxZ;

        // اگر مقادیر حدی به‌درستی تعیین نشده‌اند، از پیش‌فرض استفاده کن
        if (startX == Float.MAX_VALUE || endX == -Float.MAX_VALUE) {
            startX = -5f;
            endX = 5f;
            Log.w(TAG, "Axis bounds using default X range.");
        }
        if (startY == Float.MAX_VALUE || endY == -Float.MAX_VALUE) {
            startY = -5f;
            endY = 5f;
            Log.w(TAG, "Axis bounds using default Y range.");
        }
        if (startZ == Float.MAX_VALUE || endZ == -Float.MAX_VALUE) {
            startZ = -1f;
            endZ = 1f;
            Log.w(TAG, "Axis bounds using default Z range.");
        }

        ArrayList<Float> gridVerticesList = new ArrayList<>();

        // شبکه XY در ارتفاع minZ و maxZ
        for (float x = startX; x <= endX; x += gridSpacingX) {
            for (float zLevel : new float[]{startZ, endZ}) { // شبکه XY در کف و سقف Z
                gridVerticesList.add(x);
                gridVerticesList.add(startY);
                gridVerticesList.add(zLevel);

                gridVerticesList.add(x);
                gridVerticesList.add(endY);
                gridVerticesList.add(zLevel);
            }
        }
        for (float y = startY; y <= endY; y += gridSpacingY) {
            for (float zLevel : new float[]{startZ, endZ}) { // شبکه XY در کف و سقف Z
                gridVerticesList.add(startX);
                gridVerticesList.add(y);
                gridVerticesList.add(zLevel);

                gridVerticesList.add(endX);
                gridVerticesList.add(y);
                gridVerticesList.add(zLevel);
            }
        }

        // شبکه XZ در ارتفاع minY و maxY
        for (float x = startX; x <= endX; x += gridSpacingX) {
            for (float yLevel : new float[]{minY, maxY}) { // شبکه XZ در مرزهای Y
                gridVerticesList.add(x);
                gridVerticesList.add(yLevel);
                gridVerticesList.add(startZ);

                gridVerticesList.add(x);
                gridVerticesList.add(yLevel);
                gridVerticesList.add(endZ);
            }
        }
        for (float z = startZ; z <= endZ; z += gridSpacingX) { // استفاده از gridSpacingX برای فواصل z همسان با x
            for (float yLevel : new float[]{minY, maxY}) { // شبکه XZ در مرزهای Y
                gridVerticesList.add(startX);
                gridVerticesList.add(yLevel);
                gridVerticesList.add(z);

                gridVerticesList.add(endX);
                gridVerticesList.add(yLevel);
                gridVerticesList.add(z);
            }
        }

        // شبکه YZ در ارتفاع minX و maxX
        for (float y = startY; y <= endY; y += gridSpacingY) {
            for (float xLevel : new float[]{minX, maxX}) { // شبکه YZ در مرزهای X
                gridVerticesList.add(xLevel);
                gridVerticesList.add(y);
                gridVerticesList.add(startZ);

                gridVerticesList.add(xLevel);
                gridVerticesList.add(y);
                gridVerticesList.add(endZ);
            }
        }
        for (float z = startZ; z <= endZ; z += gridSpacingX) { // استفاده از gridSpacingX برای فواصل z همسان با x
            for (float xLevel : new float[]{minX, maxX}) { // شبکه YZ در مرزهای X
                gridVerticesList.add(xLevel);
                gridVerticesList.add(startY);
                gridVerticesList.add(z);

                gridVerticesList.add(xLevel);
                gridVerticesList.add(endY);
                gridVerticesList.add(z);
            }
        }

        // تبدیل لیست به آرایه float
        int totalVertices = gridVerticesList.size() / 3;
        float[] gridVertices = new float[gridVerticesList.size()];
        for (int i = 0; i < gridVerticesList.size(); i++) {
            gridVertices[i] = gridVerticesList.get(i);
        }

        ByteBuffer vbb = ByteBuffer.allocateDirect(gridVertices.length * Float.BYTES);
        vbb.order(ByteOrder.nativeOrder());
        axisVertexBuffer = vbb.asFloatBuffer();
        axisVertexBuffer.put(gridVertices);
        axisVertexBuffer.position(0);

        axisVertexCount = totalVertices;

        if (axisVertexBuffer == null) {
            Log.e(TAG, "Error creating axis vertex buffer!");
        } else {
            Log.d(TAG, "Axis vertex buffer created successfully.");
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;

        // به‌روزرسانی مرکز گراف
        updateCenter();

        // محاسبه ابعاد گراف
        float graphWidth = maxX - minX;
        float graphHeight = maxY - minY;
        float graphDepth = maxZ - minZ;
        float aspect = (float) width / height;
        float dataAspect = graphWidth / graphHeight;

        // تنظیمات بر اساس نوع گراف
        switch (currentGraphType) {
            case LINE_GRAPH:
                float windowWidth = 10.0f;
                if (is2DMode) {
                    float margin = (maxY - minY) * 2.5f;
                    Matrix.orthoM(projectionMatrix, 0,
                            0.0f * zoomFactor + xOffset2D,
                            windowWidth * zoomFactor + xOffset2D,
                            (minY - margin) * zoomFactor + yOffset2D,
                            (maxY + margin) * zoomFactor + yOffset2D,
                            -100f, 100f);

                    Matrix.setLookAtM(viewMatrix, 0,
                            windowWidth/2, centerY, fixedCameraZ_TopDown,
                            windowWidth/2, centerY, 0f,
                            0f, 1.0f, 0.0f);
                } else {
                    Matrix.frustumM(projectionMatrix, 0,
                            -ratio, ratio, -1, 1, 1f, 500f);
                    Matrix.setLookAtM(viewMatrix, 0,
                            centerX - 0f, centerY - cameraY_3D, centerZ - zoom,
                            centerX, centerY, centerZ,
                            0f, 1.0f, 0.0f);
                }
                break;

            case POINT_MODE:
                if (is2DMode) {
                    windowWidth = 10.0f;
                    // استفاده از مقیاس ثابت برای نمایش مثلث
                    Matrix.orthoM(projectionMatrix, 0,
                            0.0f * zoomFactor + xOffset2D,
                            windowWidth * zoomFactor + xOffset2D,
                            -5.0f * zoomFactor + yOffset2D,
                            5.0f * zoomFactor + yOffset2D,
                            -100f, 100f);

                    Matrix.setLookAtM(viewMatrix, 0,
                            windowWidth/2, 0f, fixedCameraZ_TopDown,
                            windowWidth/2, 0f, 0f,
                            0f, 1.0f, 0.0f);
                } else {
                    Matrix.frustumM(projectionMatrix, 0,
                            -ratio, ratio, -1, 1, 1f, 500f);
                    Matrix.setLookAtM(viewMatrix, 0,
                            centerX, centerY, centerZ - zoom,
                            centerX, centerY, centerZ,
                            0f, 1.0f, 0.0f);
                }
                break;

            case SURFACE_3D:
            default:
                if (is2DMode) {
                    if (graphWidth <= 0 || graphHeight <= 0) {
                        Matrix.orthoM(projectionMatrix, 0,
                                -1 * zoomFactor + xOffset2D,
                                1 * zoomFactor + xOffset2D,
                                -1 * zoomFactor + yOffset2D,
                                1 * zoomFactor + yOffset2D,
                                -100f, 100f);
                    } else if (dataAspect > aspect) {
                        Matrix.orthoM(projectionMatrix, 0,
                                -graphWidth / 2 * zoomFactor + xOffset2D,
                                graphWidth / 2 * zoomFactor + xOffset2D,
                                -graphWidth / (2 * aspect) * zoomFactor + yOffset2D,
                                graphWidth / (2 * aspect) * zoomFactor + yOffset2D,
                                -100f, 100f);
                    } else {
                        Matrix.orthoM(projectionMatrix, 0,
                                -graphHeight * aspect / 2 * zoomFactor + xOffset2D,
                                graphHeight * aspect / 2 * zoomFactor + xOffset2D,
                                -graphHeight / 2 * zoomFactor + yOffset2D,
                                graphHeight / 2 * zoomFactor + yOffset2D,
                                -100f, 100f);
                    }

                    float lookAtZ = centerZ + fixedCameraZ_TopDown;
                    Matrix.setLookAtM(viewMatrix, 0,
                            centerX + xOffset2D,
                            centerY + yOffset2D,
                            lookAtZ,
                            centerX + xOffset2D,
                            centerY + yOffset2D,
                            centerZ,
                            0f, 1.0f, 0.0f);
                } else {
                    Matrix.frustumM(projectionMatrix, 0,
                            -ratio, ratio, -1, 1, 1f, 500f);
                    Matrix.setLookAtM(viewMatrix, 0,
                            centerX - 0f,
                            centerY - cameraY_3D,
                            centerZ - zoom,
                            centerX, centerY, centerZ,
                            0f, 1.0f, 0.0f);
                }
                break;
        }

        Log.d(TAG, String.format(
                "Surface Changed - Mode: %s, Type: %s, Dimensions: %dx%d, Zoom: %.2f",
                is2DMode ? "2D" : "3D",
                currentGraphType.toString(),
                width, height,
                zoomFactor
        ));
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // تنظیم ماتریس‌ها
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.setIdentityM(viewMatrix, 0);
        Matrix.translateM(viewMatrix, 0, 0f, 0f, zoom);
        if (!is2DMode) {

            // محاسبه ماتریس نهایی MVP
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
            Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
            Matrix.rotateM(viewMatrix, 0, yRotation, 1.0f, 0.0f, 0.0f);
            Matrix.rotateM(viewMatrix, 0, xRotation, 0.0f, 1.0f, 0.0f);
            Matrix.translateM(viewMatrix, 0, -centerX, -centerY, -centerZ);
        } else {
            Matrix.setLookAtM(viewMatrix, 0,
                    centerX + xOffset2D, centerY + yOffset2D, centerZ + fixedCameraZ_TopDown,
                    centerX + xOffset2D, centerY + yOffset2D, centerZ,
                    0f, 1.0f, 0.0f);
        }

        // محاسبه ماتریس نهایی MVP
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0);

        // رسم بر اساس نوع نمودار
        switch (currentGraphType) {
            case SURFACE_3D:
                drawSurface();
                if (isGridVisible) {
                    drawAxes();
                }
                if (is2DMode && isGraphBuilt && isPlusModeEnabled) {
                    drawPlus();
                }
                break;

            case LINE_GRAPH:
                drawLineGraph();
                break;

            case POINT_MODE:
                if (dataPoints != null && !dataPoints.isEmpty()) {
                    generateTriangleVertices();
                    drawTriangle();
                } else {
                    Log.w(TAG, "No data points available for POINT_MODE");
                }
                break;
        }

        // بررسی خطاهای OpenGL
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "OpenGL error in onDrawFrame: " + error);
        }
    }

    private void drawSurface() {
        if (surfaceVertexBuffer == null || surfaceIndexBuffer == null) {
            Log.e(TAG, "Vertex or Index buffer is null, skipping surface draw.");
            return;
        }

        GLES20.glUseProgram(surfaceProgram);
        GLES20.glUniformMatrix4fv(surfaceMvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(surfaceBasePointHandle, basePointValue);
        rVisibilityHandle = GLES20.glGetUniformLocation(surfaceProgram, "RVisibility");
        gVisibilityHandle = GLES20.glGetUniformLocation(surfaceProgram, "GVisibility");
        bVisibilityHandle = GLES20.glGetUniformLocation(surfaceProgram, "BVisibility");
        int basePointHandle = GLES20.glGetUniformLocation(surfaceProgram, "u_BasePoint");
        int minBasePointHandle = GLES20.glGetUniformLocation(surfaceProgram, "u_minZ");
        int maxBasePointHandle = GLES20.glGetUniformLocation(surfaceProgram, "u_maxZ");

        if (DataProcessor.scanReady) {
            GLES20.glUniform1f(basePointHandle, basePointValue);
        }
        Log.i(TAG, "basePointValue: " + basePointValue);
        GLES20.glUniform1f(minBasePointHandle, 0); // حداقل مقدار مبنا را تنظیم کنید
        GLES20.glUniform1f(maxBasePointHandle, 10); // حداکثر مقدار مبنا را تنظیم کنید
        GLES20.glUniform1i(rVisibilityHandle, RVisibility ? 1 : 0);
        GLES20.glUniform1i(gVisibilityHandle, GVisibility ? 1 : 0);
        GLES20.glUniform1i(bVisibilityHandle, BVisibility ? 1 : 0);

        surfaceVertexBuffer.position(0);
        surfaceIndexBuffer.position(0);

        GLES20.glVertexAttribPointer(surfacePositionHandle, 3, GLES20.GL_FLOAT, false, 0, surfaceVertexBuffer);
        GLES20.glEnableVertexAttribArray(surfacePositionHandle);

        for (int i = 0; i < numIndices; i += 4) {
            GLES20.glDrawElements(GLES20.GL_TRIANGLE_FAN, 4, GLES20.GL_UNSIGNED_SHORT, surfaceIndexBuffer.position(i));
        }

        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "glDrawElements surface error: " + error);
        } else {
            Log.d(TAG, "Surface drawn successfully.");
        }

        GLES20.glDisableVertexAttribArray(surfacePositionHandle);
    }

    private void drawAxes() {
        GLES20.glUseProgram(axisProgram);
        GLES20.glUniformMatrix4fv(axisMvpMatrixHandle, 1, false, mvpMatrix, 0);

        axisVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(axisPositionHandle, 3, GLES20.GL_FLOAT, false, 0, axisVertexBuffer);
        GLES20.glEnableVertexAttribArray(axisPositionHandle);

        // تنظیم رنگ به سفید
        float[] whiteColor = {1.0f, 1.0f, 1.0f, 1.0f};
        GLES20.glUniform4fv(axisColorHandle, 1, whiteColor, 0);

        GLES20.glLineWidth(2.0f);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, axisVertexCount);

        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "glDrawArrays axes error: " + error);
        } else {
            Log.d(TAG, "Axes drawn successfully.");
        }

        GLES20.glDisableVertexAttribArray(axisPositionHandle);
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        final int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader " + type + ":" + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    public void onTouchEvent(float deltaX, float deltaY, boolean isZoom) {
        if (isGraphBuilt) {
            if (isPlusModeEnabled) {
                handleTouchInput(deltaX, deltaY);
                // بنابراین در اینجا نیازی به انجام کاری نیست.
                return; // **اضافه شده**: برای خروج از متد در حالت PlusMode
            } else { // اگر حالت نشانه گر + غیر فعال است (منطق قبلی برای زوم و چرخش)
                if (is2DMode) {
                    if (isZoom) {
                        this.zoomFactor -= deltaY * 0.01f;
                        this.zoomFactor = Math.max(0.1f, Math.min(zoomFactor, 5.0f));
                        onSurfaceChanged(null, surfaceWidth, surfaceHeight);
                        Log.d(TAG, "2D Zoom: zoomFactor=" + zoomFactor);
                    } else {
                        this.xOffset2D += deltaX * 0.005f * zoomFactor;
                        this.yOffset2D += deltaY * 0.005f * zoomFactor;
                        onSurfaceChanged(null, surfaceWidth, surfaceHeight);
                        Log.d(TAG, "2D Pan: xOffset2D=" + xOffset2D + ", yOffset2D=" + yOffset2D);
                    }
                } else { // حالت 3 بعدی - بدون تغییر
                    if (isZoom) {
                        this.zoom += deltaY * 0.1f;
                        float minZoom = -250f;
                        float maxZoom = 1000f;
                        this.zoom = Math.max(minZoom, Math.min(zoom, maxZoom));
                        Log.d(TAG, "3D Zoom in touch: zoom=" + zoom);
                    } else {
                        xRotation += deltaX * 0.5f;
                        yRotation += deltaY * 0.5f;
                    }
                }
            }
        }
    }

    public void setGridVisibility(boolean visible) {
        isGridVisible = visible;
    }

    private void setup2DMode() {
        is2DMode = true; // اطمینان از تنظیم صحیح وضعیت 2 بعدی

        zoomFactor = 1.0f; // بازنشانی zoomFactor به مقدار پیش‌فرض برای 2 بعدی
        xOffset2D = 0f;    // بازنشانی xOffset2D به صفر برای 2 بعدی
        yOffset2D = 0f;    // بازنشانی yOffset2D به صفر برای 2 بعدی

        // تنظیم ماتریس View برای حالت 2 بعدی (مشابه onDrawFrame در حالت 2 بعدی)
        Matrix.setLookAtM(viewMatrix, 0,
                centerX + xOffset2D, centerY + yOffset2D, centerZ + fixedCameraZ_TopDown,
                centerX + xOffset2D, centerY + yOffset2D, centerZ,
                0f, 1.0f, 0.0f);

        Log.d(TAG, "setup2DMode: Zoom and ViewMatrix for 2D mode reset.");
        onSurfaceChanged(null, surfaceWidth, surfaceHeight); // به‌روزرسانی پرسپکتیو برای حالت 2 بعدی
    }

    private void setup3DMode() {
        is2DMode = false; // اطمینان از تنظیم صحیح وضعیت 3 بعدی

        zoom = -15f;      // بازنشانی zoom به مقدار پیش‌فرض برای 3 بعدی

        // تنظیم ماتریس View برای حالت 3 بعدی (تنظیم اولیه دوربین در onSurfaceChanged)
        Matrix.setLookAtM(viewMatrix, 0,
                0f, cameraY_3D, 0f, // موقعیت اولیه دوربین
                centerX, centerY, centerZ, // نگاه به مرکز شیء
                0f, 1.0f, 0.0f);

        Log.d(TAG, "setup3DMode: Zoom and ViewMatrix for 3D mode reset.");
        onSurfaceChanged(null, surfaceWidth, surfaceHeight); // به‌روزرسانی پرسپکتیو برای حالت 3 بعدی
    }

    public void toggleMode() {
        is2DMode = !is2DMode; // تغییر وضعیت 2D/3D

        if (is2DMode) {
            setup2DMode(); // تنظیم حالت 2 بعدی
        } else {
            setup3DMode(); // تنظیم حالت 3 بعدی
        }

        Log.d(TAG, "Mode toggled to " + (is2DMode ? "2D" : "3D"));
    }

    public void addDataPoint(float[] newDataPoint) {
        if (newDataPoint == null || newDataPoint.length < 3) {
            return;
        }
        dataPoints.add(newDataPoint);

        switch (currentGraphType) {
            case LINE_GRAPH:
                generateLineGraphVertices();
                break;
            case POINT_MODE:
                generateTriangleVertices();
                break;
            case SURFACE_3D:
                generateSurfaceVerticesFromData();
                break;
        }

        calculateDataBounds();
        generatePlusVertices();
        generateAxisVertices();
    }

    private void updateCenter() {
        centerX = (minX + maxX) / 2f;
        centerY = (minY + maxY) / 2f;
        centerZ = (minZ + maxZ) / 2f;
    }

    public float adjustZValue(float zValue) {
        return zValue - basePointValue;
    }

    private void generateLineGraphVertices() {
        if (dataPoints == null || dataPoints.isEmpty()) {
            return;
        }

        List<Float> lineVertices = new ArrayList<>();

        // پنجره ثابت برای نمایش (مثلاً آخرین 100 نقطه)
        float windowWidth = 10.0f; // عرض ثابت پنجره نمایش
        float startX = windowWidth; // شروع از سمت راست پنجره

        // محاسبه مرکز محور Y و مقیاس برای حرکت عمودی
        float centerY = (maxY + minY) / 2.0f;
        float zScale = (maxY - minY) / 4.0f; // مقیاس برای محدود کردن حرکت عمودی

        // محاسبه فاصله بین نقاط
        float xStep = windowWidth / 100.0f; // فاصله ثابت بین نقاط
        float currentX = startX;

        // محاسبه اندیس شروع (برای نمایش آخرین نقاط)
        int startIndex = Math.max(0, dataPoints.size() - 100);

        // اضافه کردن نقاط
        for (int i = startIndex; i < dataPoints.size(); i++) {
            float[] point = dataPoints.get(i);
            if (point != null && point.length >= 3) {
                float zValue = point[2];
                // محاسبه موقعیت Y بر اساس اختلاف Z با basePoint
                float offsetFromBase = (zValue - basePointValue) * zScale;
                float yPosition = centerY + offsetFromBase;

                lineVertices.add(currentX);     // X - موقعیت ثابت در پنجره
                lineVertices.add(yPosition);    // Y - موقعیت نسبت به مرکز
                lineVertices.add(zValue);       // Z - مقدار اصلی برای رنگ‌آمیزی

                currentX -= xStep;
            }
        }

        float[] vertices = new float[lineVertices.size()];
        for (int i = 0; i < lineVertices.size(); i++) {
            vertices[i] = lineVertices.get(i);
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        lineVertexBuffer = bb.asFloatBuffer();
        lineVertexBuffer.put(vertices);
        lineVertexBuffer.position(0);

        lineVertexCount = vertices.length / 3;
    }

    private void drawLineGraph() {
        if (lineVertexBuffer == null || lineVertexCount == 0) {
            return;
        }

        GLES20.glUseProgram(surfaceProgram);
        GLES20.glUniformMatrix4fv(surfaceMvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(surfaceBasePointHandle, basePointValue);
        rVisibilityHandle = GLES20.glGetUniformLocation(surfaceProgram, "RVisibility");
        gVisibilityHandle = GLES20.glGetUniformLocation(surfaceProgram, "GVisibility");
        bVisibilityHandle = GLES20.glGetUniformLocation(surfaceProgram, "BVisibility");
        int basePointHandle = GLES20.glGetUniformLocation(surfaceProgram, "u_BasePoint");
        int minBasePointHandle = GLES20.glGetUniformLocation(surfaceProgram, "u_minZ");
        int maxBasePointHandle = GLES20.glGetUniformLocation(surfaceProgram, "u_maxZ");

        GLES20.glUniform1f(basePointHandle, basePointValue);

        Log.i(TAG, "basePointValue: " + basePointValue);
        GLES20.glUniform1f(minBasePointHandle, 0); // حداقل مقدار مبنا را تنظیم کنید
        GLES20.glUniform1f(maxBasePointHandle, 10); // حداکثر مقدار مبنا را تنظیم کنید
        GLES20.glUniform1i(rVisibilityHandle, RVisibility ? 1 : 0);
        GLES20.glUniform1i(gVisibilityHandle, GVisibility ? 1 : 0);
        GLES20.glUniform1i(bVisibilityHandle, BVisibility ? 1 : 0);


        lineVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(surfacePositionHandle, 3, GLES20.GL_FLOAT,
                false, 0, lineVertexBuffer);
        GLES20.glEnableVertexAttribArray(surfacePositionHandle);

        // تنظیم ضخامت خط
        GLES20.glLineWidth(3.0f);

        // رسم نمودار خطی به صورت متصل
        GLES20.glDrawArrays(GL_LINE_STRIP, 0, lineVertexCount);

        GLES20.glDisableVertexAttribArray(surfacePositionHandle);
    }

    public void setGraphType(GraphType type) {
        this.currentGraphType = type;
        calculateDataBounds();

        switch (type) {
            case LINE_GRAPH:
                generateLineGraphVertices();
                break;
            case POINT_MODE:
               generateTriangleVertices();
                break;
            case SURFACE_3D:
                generateSurfaceVerticesFromData();
                break;
        }

        generateAxisVertices();
    }

    public GraphType getCurrentGraphType() {
        return currentGraphType;
    }

    public void updateBasePoint(float newBasePoint) {
        this.basePointValue = newBasePoint;
        if (currentGraphType == GraphType.LINE_GRAPH) {
            generateLineGraphVertices();
        }
    }

    public void releaseResources() {
        Log.d(TAG, "Releasing resources...");

        // آزاد سازی بافرها
        if (surfaceVertexBuffer != null) {
            surfaceVertexBuffer.clear();
            surfaceVertexBuffer = null;
        }
        if (surfaceIndexBuffer != null) {
            surfaceIndexBuffer.clear();
            surfaceIndexBuffer = null;
        }
        if (lineVertexBuffer != null) {
            lineVertexBuffer.clear();
            lineVertexBuffer = null;
        }
        if (axisVertexBuffer != null) {
            axisVertexBuffer.clear();
            axisVertexBuffer = null;
        }
        if (triangleVertexBuffer != null) {
            triangleVertexBuffer.clear();
            triangleVertexBuffer = null;
        }

        // حذف برنامه‌های شیدر
        if (surfaceProgram != 0) {
            GLES20.glDeleteProgram(surfaceProgram);
            surfaceProgram = 0;
        }
        if (axisProgram != 0) {
            GLES20.glDeleteProgram(axisProgram);
            axisProgram = 0;
        }

        // پاکسازی لیست‌ها و متغیرها
        if (dataPoints != null) {
            dataPoints.clear();
            dataPoints = null;
        }
        lineVertexCount = 0;
        axisVertexCount = 0;
        isGraphBuilt = false;

        Log.d(TAG, "Resources released.");
    }

    public void adjustBasePoint(String adjustmentType) {
        if (adjustmentType.equals("+")) {
            basePointValue += 0.5f; // افزایش basePoint
        } else if (adjustmentType.equals("-")) {
            basePointValue -= 0.5f; // کاهش basePoint
        }

        // محدود کردن مقدار basePoint به بازه 0 تا 10
        basePointValue = Math.max(0.0f, Math.min(10.0f, basePointValue));

        // **نکته مهم:** در اینجا نیازی به تنظیم یونیفرم نیست.
        // یونیفرم باید در متد onDrawFrame تنظیم شود تا در فریم بعدی اعمال شود.
    }

    private void initPlusShaders() {
        String vertexShaderCode =
                "uniform mat4 u_MVPMatrix;" +
                        "attribute vec4 vPosition;" +
                        "uniform vec3 u_PlusTranslation;" + // **اضافه کردن uniform u_PlusTranslation**
                        "void main() {" +
                        "  gl_Position = u_MVPMatrix * (vPosition + vec4(u_PlusTranslation, 0.0));" + // **اعمال انتقال با u_PlusTranslation**
                        "}";

        String fragmentShaderCode =

                "precision mediump float;" +

                        "uniform vec4 u_Color;" +

                        "void main() {" +

                        " gl_FragColor = u_Color;" +

                        "}";

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        plusProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(plusProgram, vertexShader);
        GLES20.glAttachShader(plusProgram, fragmentShader);
        GLES20.glLinkProgram(plusProgram);

        final int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(plusProgram, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Error linking plus shader program: " + GLES20.glGetProgramInfoLog(plusProgram));
            GLES20.glDeleteProgram(plusProgram);
            plusProgram = 0;
        } else {
            plusPositionHandle = GLES20.glGetAttribLocation(plusProgram, "vPosition");
            plusColorHandle = GLES20.glGetUniformLocation(plusProgram, "u_Color");
            plusMvpMatrixHandle = GLES20.glGetUniformLocation(plusProgram, "u_MVPMatrix");
            plusTranslationHandle = GLES20.glGetUniformLocation(plusProgram, "u_PlusTranslation"); // **دریافت handle برای uniform u_PlusTranslation**
        }
    }

    private void generatePlusVertices() {
        float[] vertices = {
                -plusSize, 0.0f, 0.0f, // 1
                plusSize, 0.0f, 0.0f,  // 2
                0.0f, -plusSize, 0.0f, // 3
                0.0f, plusSize, 0.0f,  // 4
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        plusVertexBuffer = bb.asFloatBuffer();
        plusVertexBuffer.put(vertices);
        plusVertexBuffer.position(0);
    }

    public void setPlusPosition(float x, float y) {
        plusPosition[0] = x;
        plusPosition[1] = y;
    }

    private void drawPlus() {
        if (plusProgram == 0 || plusVertexBuffer == null) {
            return;
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST); // **غیرفعال کردن تست عمق قبل از رسم +**
        GLES20.glUseProgram(plusProgram);
        GLES20.glUniformMatrix4fv(plusMvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform4fv(plusColorHandle, 1, plusColor, 0);
        GLES20.glUniform3fv(plusTranslationHandle, 1, plusPosition, 0);

        plusVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(plusPositionHandle, 3, GLES20.GL_FLOAT, false, 0, plusVertexBuffer);
        GLES20.glEnableVertexAttribArray(plusPositionHandle);

        // رسم دو خط برای ایجاد علامت "+"
        GLES20.glLineWidth(5.0f);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2);
        GLES20.glDrawArrays(GLES20.GL_LINES, 2, 2);

        GLES20.glDisableVertexAttribArray(plusPositionHandle);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST); // **فعال کردن مجدد تست عمق بعد از رسم +**
    }

    public void handleTouchInput(float screenX, float screenY) {
        if (!is2DMode || !isPlusModeEnabled) return;

        // تبدیل مختصات صفحه نمایش به مختصات نرمال شده
        float normalizedX = (screenX / surfaceWidth) * 2 - 1;
        float normalizedY = -((screenY / surfaceHeight) * 2 - 1);

        // تبدیل به مختصات دنیای واقعی
        float worldX = minX + ((normalizedX + 1) / 2) * (maxX - minX);
        float worldY = minY + ((normalizedY + 1) / 2) * (maxY - minY);

        // محدود کردن مختصات به محدوده داده‌ها
        worldX = Math.max(minX, Math.min(maxX, worldX));
        worldY = Math.max(minY, Math.min(maxY, worldY));

        // تنظیم موقعیت +
        setPlusPosition(worldX, worldY);

        // محاسبه مقدار Z
        float zValue = getZValueForTouch(worldX, worldY);
        plusPosition[2] = zValue;

        Log.d(TAG, String.format("Touch on surface at (%.2f, %.2f) -> Z: %.2f",
                worldX, worldY, zValue * 32768 / 10));
        if (zValueListener != null) {
            zValueListener.onZValueCalculated(zValue);
        }


    }

    // تعریف یک اینترفیس برای ارسال مقدار Z به اکتیویتی
    public interface OnZValueCalculatedListener {
        void onZValueCalculated(float zValue);
    }

    private OnZValueCalculatedListener zValueListener;

    // متدی برای تنظیم listener
    public void setOnZValueCalculatedListener(OnZValueCalculatedListener listener) {
        this.zValueListener = listener;
    }

    private void updatePlusSize() {
        plusSize = 0.5f * zoomFactor;
        generatePlusVertices();
    }

    private float getZValueForTouch(float worldX, float worldY) {
        if (dataPoints == null || dataPoints.isEmpty()) {
            return 0;
        }

        // پیدا کردن نزدیک‌ترین نقاط گرید به نقطه لمس شده
        int gridX = (int) ((worldX - minX) / gridSpacingX);
        int gridY = (int) ((worldY - minY) / gridSpacingY);

        // محاسبه شاخص‌های چهار نقطه اطراف نقطه لمس شده
        int index00 = gridY * gridSizeX + gridX;
        int index10 = gridY * gridSizeX + (gridX + 1);
        int index01 = (gridY + 1) * gridSizeX + gridX;
        int index11 = (gridY + 1) * gridSizeX + (gridX + 1);

        // بررسی اعتبار شاخص‌ها
        if (index00 >= 0 && index11 < dataPoints.size() &&
                gridX >= 0 && gridX < gridSizeX - 1 &&
                gridY >= 0 && gridY < gridSizeY - 1) {

            // محاسبه ضرایب درون‌یابی
            float dx = (worldX - (minX + gridX * gridSpacingX)) / gridSpacingX;
            float dy = (worldY - (minY + gridY * gridSpacingY)) / gridSpacingY;

            // دریافت مقادیر Z چهار نقطه اطراف
            float z00 = dataPoints.get(index00)[2];
            float z10 = dataPoints.get(index10)[2];
            float z01 = dataPoints.get(index01)[2];
            float z11 = dataPoints.get(index11)[2];

            // درون‌یابی دو خطی
            float zx0 = z00 * (1 - dx) + z10 * dx;
            float zx1 = z01 * (1 - dx) + z11 * dx;
            float z = zx0 * (1 - dy) + zx1 * dy;

            return z;
        }

        // اگر نقطه خارج از محدوده باشد، نزدیک‌ترین نقطه معتبر را برگردان
        for (float[] point : dataPoints) {
            if (point != null && point.length >= 3) {
                float distance = (float) Math.sqrt(
                        Math.pow(point[0] - worldX, 2) +
                                Math.pow(point[1] - worldY, 2));
                if (distance < gridSpacingX) {
                    return point[2];
                }
            }
        }

        return 0;
    }

    private void calculateDataBounds() {
        if (dataPoints == null || dataPoints.isEmpty()) {
            return;
        }

        for (float[] point : dataPoints) {
            if (point != null && point.length >= 2) { // حداقل X و Y باید وجود داشته باشد
                float x = point[0];
                float y = point[1];

                minXData = Math.min(minXData, x);
                maxXData = Math.max(maxXData, x);
                minYData = Math.min(minYData, y);
                maxYData = Math.max(maxYData, y);
            }
        }
        Log.d(TAG, "محدوده داده ها: X=[" + minXData + ", " + maxXData + "], Y=[" + minYData + ", " + maxYData + "]"); // لاگ محدوده داده ها
    }

    public boolean isTouchOnSurface(float screenX, float screenY) {
        // تبدیل مختصات صفحه نمایش به مختصات نرمال شده
        float normalizedX = (screenX / surfaceWidth) * 2 - 1;
        float normalizedY = -((screenY / surfaceHeight) * 2 - 1);

        // تبدیل به مختصات دنیای واقعی
        float worldX = minX + ((normalizedX + 1) / 2) * (maxX - minX);
        float worldY = minY + ((normalizedY + 1) / 2) * (maxY - minY);

        // بررسی محدوده مجاز
        return worldX >= minX && worldX <= maxX &&
                worldY >= minY && worldY <= maxY;
    }
    private void generateTriangleVertices() {
        if (dataPoints == null || dataPoints.isEmpty()) {
            return;
        }

        // دریافت آخرین نقطه از لیست
        float[] lastPoint = dataPoints.get(dataPoints.size() - 1);
        float x = lastPoint[0];
        float baseY = 0f; // مرکز صفحه
        float zValue = lastPoint[2] - basePointValue; // تنظیم Z نسبت به basePoint

        // محاسبه ارتفاع مثلث بر اساس مقدار Z
        float heightScale = Math.abs(zValue) * 0.5f;
        float direction = Math.signum(zValue);

        // رئوس مثلث
        float[] triangleCoords = {
                x - triangleBaseWidth/2, baseY, zValue,  // چپ
                x + triangleBaseWidth/2, baseY, zValue,  // راست
                x, baseY + direction * triangleHeight * heightScale, zValue  // رأس
        };

        // ایجاد بافر برای رئوس
        ByteBuffer bb = ByteBuffer.allocateDirect(triangleCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        triangleVertexBuffer = bb.asFloatBuffer();
        triangleVertexBuffer.put(triangleCoords);
        triangleVertexBuffer.position(0);
    }
    private void drawTriangle() {
        if (triangleVertexBuffer == null) return;

        GLES20.glUseProgram(surfaceProgram);
        GLES20.glUniformMatrix4fv(surfaceMvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(surfaceBasePointHandle, basePointValue);
        rVisibilityHandle = GLES20.glGetUniformLocation(surfaceProgram, "RVisibility");
        gVisibilityHandle = GLES20.glGetUniformLocation(surfaceProgram, "GVisibility");
        bVisibilityHandle = GLES20.glGetUniformLocation(surfaceProgram, "BVisibility");
        int basePointHandle = GLES20.glGetUniformLocation(surfaceProgram, "u_BasePoint");
        int minBasePointHandle = GLES20.glGetUniformLocation(surfaceProgram, "u_minZ");
        int maxBasePointHandle = GLES20.glGetUniformLocation(surfaceProgram, "u_maxZ");

        if (DataProcessor.scanReady) {
            GLES20.glUniform1f(basePointHandle, basePointValue);
        }
        Log.i(TAG, "basePointValue: " + basePointValue);
        GLES20.glUniform1f(minBasePointHandle, 0); // حداقل مقدار مبنا را تنظیم کنید
        GLES20.glUniform1f(maxBasePointHandle, 10); // حداکثر مقدار مبنا را تنظیم کنید
        GLES20.glUniform1i(rVisibilityHandle, RVisibility ? 1 : 0);
        GLES20.glUniform1i(gVisibilityHandle, GVisibility ? 1 : 0);
        GLES20.glUniform1i(bVisibilityHandle, BVisibility ? 1 : 0);

        triangleVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(surfacePositionHandle, 3, GLES20.GL_FLOAT,
                false, 0, triangleVertexBuffer);
        GLES20.glEnableVertexAttribArray(surfacePositionHandle);

        // رسم مثلث
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);

        GLES20.glDisableVertexAttribArray(surfacePositionHandle);
    }
    public void setTriangleSize(float baseWidth, float height) {
        this.triangleBaseWidth = baseWidth;
        this.triangleHeight = height;
    }

}
