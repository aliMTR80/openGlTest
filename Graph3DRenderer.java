package com.novinsadr.myapplication;

import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

public class Graph3DRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "Graph3DRenderer";

    // Matrix constants
    private float[] mRotationMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mModelMatrix = new float[16];

    // View modes
    public static final byte VIEW_CUSTOM = 0;
    public static final byte VIEW_TOP = 1;
    public static final byte VIEW_SIDE = 2;
    public static final byte VIEW_PERSPECTIVE = 3;
    private byte mCurrentView = VIEW_TOP;

    // Buffer constants
    private static final int COORDS_PER_VERTEX = 3;
    private static final int VALUES_PER_COLOR = 4;
    private static final int VERTEX_STRIDE = COORDS_PER_VERTEX * 4;
    private static final int COLOR_STRIDE = VALUES_PER_COLOR * 4;

    // Buffers for rendering
    private FloatBuffer mVertexBuffer;
    private FloatBuffer mColorBuffer;
    private ShortBuffer mIndexBuffer;
    private FloatBuffer mAxisBuffer;
    private FloatBuffer mGridBuffer;

    // Data dimensions
    private int mDataWidth = 0;
    private int mDataHeight = 0;
    private int mVertexCount = 0;
    private int mIndexCount = 0;

    // Transform parameters
    private float mAngleX = 0;
    private float mAngleY = 0;
    private float mAngleZ = 0;
    private float mZoom = -5.0f;
    private float mTranslateX = 0;
    private float mTranslateY = 0;
    private float mTranslateZ = 0;
    private float mScaleX = 1.0f;
    private float mScaleY = 1.0f;
    private float mScaleZ = 1.0f;

    // Data bounds
    private float mMinX = Float.MAX_VALUE;
    private float mMaxX = Float.MIN_VALUE;
    private float mMinY = Float.MAX_VALUE;
    private float mMaxY = Float.MIN_VALUE;
    private float mMinZ = Float.MAX_VALUE;
    private float mMaxZ = Float.MIN_VALUE;

    // Rendering options
    private boolean mShowGrid = true;
    private boolean mShowAxes = true;
    private int mRenderMode = GL10.GL_TRIANGLES; // Can be GL_POINTS, GL_LINES, GL_TRIANGLES

    public Graph3DRenderer() {
        Matrix.setIdentityM(mModelMatrix, 0);
        initBuffers();

        // تنظیمات اولیه برای نمایش بهتر
        mZoom = -10f;
        mAngleX = 45f;
        mAngleY = 45f;
        mScaleX = 2.0f;
        mScaleY = 2.0f;
        mScaleZ = 2.0f;
    }

    private void initBuffers() {
        // Initialize axis buffer
        float[] axisVertices = {
                0,0,0, 1,0,0,  // X axis (red)
                0,0,0, 0,1,0,  // Y axis (green)
                0,0,0, 0,0,1   // Z axis (blue)
        };
        ByteBuffer axisBB = ByteBuffer.allocateDirect(axisVertices.length * 4);
        axisBB.order(ByteOrder.nativeOrder());
        mAxisBuffer = axisBB.asFloatBuffer();
        mAxisBuffer.put(axisVertices);
        mAxisBuffer.position(0);

        // Initialize grid buffer
        initGridBuffer();
    }
    /**
     * Get current view mode
     * @return Current view mode (VIEW_CUSTOM, VIEW_TOP, VIEW_SIDE, or VIEW_PERSPECTIVE)
     */
    public byte getCurrentView() {
        return mCurrentView;
    }

    /**
     * Toggle grid visibility
     */
    public void toggleGrid() {
        mShowGrid = !mShowGrid;
    }

    /**
     * Toggle axes visibility
     */
    public void toggleAxes() {
        mShowAxes = !mShowAxes;
    }

    /**
     * Reset all transformations to default values
     */
    public void resetTransformation() {
        mTranslateX = 0.0f;
        mTranslateY = 0.0f;
        mTranslateZ = 0.0f;
        mAngleX = 0.0f;
        mAngleY = 0.0f;
        mAngleZ = 0.0f;
        mScaleX = 1.0f;
        mScaleY = 1.0f;
        mScaleZ = 1.0f;
        mZoom = -5.0f;
        mCurrentView = VIEW_TOP;

        Matrix.setIdentityM(mModelMatrix, 0);
    }
    private void initGridBuffer() {
        ArrayList<Float> gridPoints = new ArrayList<>();
        float gridSize = 10.0f;
        float step = 1.0f;

        // Create grid lines
        for (float i = -gridSize; i <= gridSize; i += step) {
            // X axis lines
            gridPoints.add(i); gridPoints.add(0f); gridPoints.add(-gridSize);
            gridPoints.add(i); gridPoints.add(0f); gridPoints.add(gridSize);

            // Z axis lines
            gridPoints.add(-gridSize); gridPoints.add(0f); gridPoints.add(i);
            gridPoints.add(gridSize); gridPoints.add(0f); gridPoints.add(i);
        }

        float[] gridVertices = new float[gridPoints.size()];
        for (int i = 0; i < gridPoints.size(); i++) {
            gridVertices[i] = gridPoints.get(i);
        }

        ByteBuffer gridBB = ByteBuffer.allocateDirect(gridVertices.length * 4);
        gridBB.order(ByteOrder.nativeOrder());
        mGridBuffer = gridBB.asFloatBuffer();
        mGridBuffer.put(gridVertices);
        mGridBuffer.position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDepthFunc(GL10.GL_LEQUAL);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);
        float ratio = (float) width / height;

        // تنظیم پروجکشن با زاویه دید مناسب‌تر
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        GLU.gluPerspective(gl, 45.0f, ratio, 0.1f, 100.0f);

        // تنظیم دوربین در موقعیت مناسب
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
        GLU.gluLookAt(gl,
                0, 5, 5,   // موقعیت دوربین
                0, 0, 0,   // نقطه نگاه
                0, 1, 0);  // جهت بالا

        // ذخیره وضعیت اولیه ماتریس‌ها
      //  gl.glGetFloatv(GL10.GL_MODELVIEW_MATRIX, mViewMatrix, 0);
        Matrix.setIdentityM(mModelMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear buffers
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        // Set up model-view-projection matrix
        setupMVPMatrix(gl);

        // Enable necessary states
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

        // Draw grid if enabled
        if (mShowGrid) {
            drawGrid(gl);
        }

        // Draw axes if enabled
        if (mShowAxes) {
            drawAxes(gl);
        }

        // Draw surface data
        if (mVertexBuffer != null && mColorBuffer != null && mIndexBuffer != null) {
            gl.glVertexPointer(COORDS_PER_VERTEX, GL10.GL_FLOAT, VERTEX_STRIDE, mVertexBuffer);
            gl.glColorPointer(VALUES_PER_COLOR, GL10.GL_FLOAT, COLOR_STRIDE, mColorBuffer);
            gl.glDrawElements(GL10.GL_TRIANGLES, mIndexCount, GL10.GL_UNSIGNED_SHORT, mIndexBuffer);
        }

        // Disable states
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
    }

    private void setupMVPMatrix(GL10 gl) {
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        // اعمال زوم
        gl.glTranslatef(0, 0, mZoom);

        // اعمال چرخش
        gl.glRotatef(mAngleX, 1, 0, 0);
        gl.glRotatef(mAngleY, 0, 1, 0);
        gl.glRotatef(mAngleZ, 0, 0, 1);

        // اعمال انتقال
        gl.glTranslatef(mTranslateX, mTranslateY, mTranslateZ);

        // اعمال مقیاس
        gl.glScalef(mScaleX, mScaleY, mScaleZ);
    }

    private void drawGrid(GL10 gl) {
        gl.glLineWidth(1.0f);
        gl.glColor4f(0.3f, 0.3f, 0.3f, 0.5f);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mGridBuffer);
        gl.glDrawArrays(GL10.GL_LINES, 0, mGridBuffer.capacity() / 3);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }

    private void drawAxes(GL10 gl) {
        gl.glLineWidth(2.0f);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mAxisBuffer);

        // X axis - Red
        gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
        gl.glDrawArrays(GL10.GL_LINES, 0, 2);

        // Y axis - Green
        gl.glColor4f(0.0f, 1.0f, 0.0f, 1.0f);
        gl.glDrawArrays(GL10.GL_LINES, 2, 2);

        // Z axis - Blue
        gl.glColor4f(0.0f, 0.0f, 1.0f, 1.0f);
        gl.glDrawArrays(GL10.GL_LINES, 4, 2);

        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }

    private void drawDataPoints(GL10 gl) {
        if (mVertexBuffer == null || mColorBuffer == null || mIndexBuffer == null) {
            return;
        }

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

        gl.glVertexPointer(COORDS_PER_VERTEX, GL10.GL_FLOAT, VERTEX_STRIDE, mVertexBuffer);
        gl.glColorPointer(VALUES_PER_COLOR, GL10.GL_FLOAT, COLOR_STRIDE, mColorBuffer);
        gl.glDrawElements(mRenderMode, mIndexCount, GL10.GL_UNSIGNED_SHORT, mIndexBuffer);

        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
    }

    // Data update methods
// در کلاس Graph3DRenderer، متد setData را به این صورت تغییر می‌دهیم:

    public void setData(float[] data, int width, int height) {
        mDataWidth = width;
        mDataHeight = height;

        int vertexCount = width * height;
        int indexCount = (width - 1) * (height - 1) * 6;

        float[] vertices = new float[vertexCount * 3];
        float[] colors = new float[vertexCount * 4];
        short[] indices = new short[indexCount];

        // محاسبه مقیاس مناسب
        float xScale = 2.0f / (width - 1);
        float zScale = 2.0f / (height - 1);
        float yScale = 1.0f; // ضریب بزرگنمایی ارتفاع

        // ایجاد vertices
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                int vertexIndex = (z * width + x) * 3;
                int colorIndex = (z * width + x) * 4;

                vertices[vertexIndex] = (x * xScale - 1.0f);
                vertices[vertexIndex + 1] = data[z * width + x] * yScale; // اعمال ضریب بزرگنمایی
                vertices[vertexIndex + 2] = (z * zScale - 1.0f);

                // رنگ‌آمیزی براساس ارتفاع
                float normalizedHeight = (vertices[vertexIndex + 1] + 1.0f) / 2.0f;
                createColor(colors, colorIndex, normalizedHeight);
            }
        }

        // ایجاد indices
        int indexIndex = 0;
        for (int z = 0; z < height - 1; z++) {
            for (int x = 0; x < width - 1; x++) {
                short bottomLeft = (short) (z * width + x);
                short bottomRight = (short) (z * width + x + 1);
                short topLeft = (short) ((z + 1) * width + x);
                short topRight = (short) ((z + 1) * width + x + 1);

                indices[indexIndex++] = bottomLeft;
                indices[indexIndex++] = topLeft;
                indices[indexIndex++] = bottomRight;

                indices[indexIndex++] = bottomRight;
                indices[indexIndex++] = topLeft;
                indices[indexIndex++] = topRight;
            }
        }

        updateBuffers(vertices, colors, indices);
        updateDataBounds(vertices);
    }

    private void createColor(float[] colors, int offset, float normalizedHeight) {
        // ایجاد طیف رنگی از آبی (سرد) به قرمز (گرم)
        if (normalizedHeight < 0.5f) {
            float t = normalizedHeight * 2;
            colors[offset] = t;          // Red
            colors[offset + 1] = t;      // Green
            colors[offset + 2] = 1.0f;   // Blue
            colors[offset + 3] = 1.0f;   // Alpha
        } else {
            float t = (normalizedHeight - 0.5f) * 2;
            colors[offset] = 1.0f;           // Red
            colors[offset + 1] = 1.0f - t;   // Green
            colors[offset + 2] = 1.0f - t;   // Blue
            colors[offset + 3] = 1.0f;       // Alpha
        }
    }

    private void updateBuffers(float[] vertices, float[] colors, short[] indices) {
        // Vertex buffer
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        mVertexBuffer = vbb.asFloatBuffer();
        mVertexBuffer.put(vertices);
        mVertexBuffer.position(0);

        // Color buffer
        ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
        cbb.order(ByteOrder.nativeOrder());
        mColorBuffer = cbb.asFloatBuffer();
        mColorBuffer.put(colors);
        mColorBuffer.position(0);

        // Index buffer
        ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        mIndexBuffer = ibb.asShortBuffer();
        mIndexBuffer.put(indices);
        mIndexBuffer.position(0);

        mVertexCount = vertices.length / 3;
        mIndexCount = indices.length;
    }

    private void updateDataBounds(float[] vertices) {
        mMinX = mMinY = mMinZ = Float.MAX_VALUE;
        mMaxX = mMaxY = mMaxZ = Float.MIN_VALUE;

        for (int i = 0; i < vertices.length; i += 3) {
            mMinX = Math.min(mMinX, vertices[i]);
            mMaxX = Math.max(mMaxX, vertices[i]);
            mMinY = Math.min(mMinY, vertices[i + 1]);
            mMaxY = Math.max(mMaxY, vertices[i + 1]);
            mMinZ = Math.min(mMinZ, vertices[i + 2]);
            mMaxZ = Math.max(mMaxZ, vertices[i + 2]);
        }
    }

    // View control methods
    public void setViewMode(byte mode) {
        mCurrentView = mode;
        switch (mode) {
            case VIEW_TOP:
                setAngle(0, 0, 0);
                break;
            case VIEW_SIDE:
                setAngle(90, 0, 0);
                break;
            case VIEW_PERSPECTIVE:
                setAngle(45, 45, 0);
                break;
        }
    }

    public void setAngle(float angleX, float angleY, float angleZ) {
        mAngleX = angleX;
        mAngleY = angleY;
        mAngleZ = angleZ;
        mCurrentView = VIEW_CUSTOM;
    }

    public void zoom(float factor) {
        mZoom = Math.max(-50.0f, Math.min(-2.0f, mZoom + factor));
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, mZoom, 0, 0, 0, 0, 1, 0);
    }

    public void translate(float x, float y, float z) {
        mTranslateX += x;
        mTranslateY += y;
        mTranslateZ += z;
    }

    public void scale(float x, float y, float z) {
        mScaleX *= x;
        mScaleY *= y;
        mScaleZ *= z;
    }

    // Rendering options
    public void setRenderMode(int mode) {
        mRenderMode = mode;
    }

    public void setShowGrid(boolean show) {
        mShowGrid = show;
    }

    public void setShowAxes(boolean show) {
        mShowAxes = show;
    }

    // Getters for current transform state
    public float getAngleX() { return mAngleX; }
    public float getAngleY() { return mAngleY; }
    public float getAngleZ() { return mAngleZ; }
    public float getZoom() { return mZoom; }
    public float[] getModelMatrix() { return mModelMatrix; }
}