package com.okm.roveruc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;
import android.os.Environment;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/* compiled from: ScanRenderer3D.java */
/* loaded from: classes.dex */
class ScanDataObject {
    public static final byte COL_A = 3;
    public static final byte COL_B = 2;
    public static final byte COL_G = 1;
    public static final byte COL_R = 0;
    public static final int MAX_IMPULSES = 251;
    public static final int MAX_LINES = 100;
    public static final byte OPMODE_DISCRIMINATION = 2;
    public static final byte OPMODE_GROUNDSCAN = 1;
    public static final byte OPMODE_MAGNETOMETER = 0;
    public static final byte TOUCH_INNER_FRAME = 4;
    public static final byte TOUCH_OUTER_FRAME_BOTTOM = 3;
    public static final byte TOUCH_OUTER_FRAME_LEFT = 0;
    public static final byte TOUCH_OUTER_FRAME_RIGHT = 2;
    public static final byte TOUCH_OUTER_FRAME_TOP = 1;
    public static final byte TRANSFORM_ROTATE_XY = 1;
    public static final byte TRANSFORM_ROTATE_Z_DX = 2;
    public static final byte TRANSFORM_ROTATE_Z_DY = 3;
    public static final byte TRANSFORM_TRANSLATE = 0;
    public static final byte TRANSFORM_ZOOM = 4;
    public static final byte VIEW_CUSTOM = 0;
    public static final byte VIEW_PERSPECTIVE = 3;
    public static final byte VIEW_SIDE = 2;
    public static final byte VIEW_TOP = 1;
    boolean CalcNewDiff;
    float DiffValue;
    private int IndexCount;
    float LastValue;
    float Offset;
    private byte OperatingMode;
    float ScanLineMedian;
    float Startwert;
    private int VertexCount;
    boolean isFirstValue;
    private FloatBuffer mColorBuffer;
    private Context mContext;
    private DoubleBuffer mGPSdata;
    private ShortBuffer mIB;
    private ShortBuffer mIndexBuffer;
    private FloatBuffer mTB;
    private int mTextureID;
    private FloatBuffer mVB;
    private FloatBuffer mVertexBuffer;
    private byte CurrentView = 1;
    float Angle = 0.0f;
    float Ymin = Float.MAX_VALUE;
    float Ymax = Float.MIN_VALUE;
    float[] mRotationMatrix = new float[16];
    float[] mRotMatrix = new float[16];
    float zoom = 0.0f;
    float translateX = 0.0f;
    float translateY = 0.0f;
    float translateZ = 0.0f;
    float angleX = 0.0f;
    float angleY = 0.0f;
    float angleZ = 0.0f;
    float scaleX = 1.0f;
    float scaleY = 1.0f;
    float scaleZ = 1.0f;
    int ScreenWidth = 0;
    int ScreenHeight = 0;
    private byte TransformationMode = 0;
    float[] LocalX = new float[3];
    float[] LocalY = new float[3];
    float[] LocalZ = new float[3];
    float[] ObjMat = new float[16];
    float[] RotationMatrix = new float[16];
    float bLeft = 0.0f;
    float bRight = 0.0f;
    float bTop = 0.0f;
    float bBottom = 0.0f;
    float bNear = 0.0f;
    float bFar = 0.0f;
    float[] colors = new float[4];
    float[] vertex = new float[3];
    private boolean UseAndroidMagSensor = false;
    private short Impulses = 50;
    private boolean ZigZag = true;
    private boolean Automatic = true;
    private short PosX = 0;
    private short PosY = 0;
    private short IncY = 1;
    private int FieldLengthInMeters = 0;
    private int FieldLengthInImpulses = 0;
    private int ImpulsesPerMeter = 0;
    private boolean isFirstLine = true;
    private boolean isNextLine = false;
    private boolean f_canSave = false;
    private boolean IsScanning = false;
    private float PrevValue = Float.NaN;
    private float MagValue = 0.0f;
    public boolean ScanActive = false;
    float[] LastLine = new float[MAX_IMPULSES];

    ScanDataObject(byte theOperatingMode, Context context) {
        this.OperatingMode = (byte) 1;
        this.OperatingMode = theOperatingMode;
        this.mContext = context;
        ByteBuffer mByteBuffer = ByteBuffer.allocateDirect(301200);
        mByteBuffer.order(ByteOrder.nativeOrder());
        this.mVertexBuffer = mByteBuffer.asFloatBuffer();
        this.mVertexBuffer.position(0);
        ByteBuffer mByteBuffer1 = ByteBuffer.allocateDirect(401600);
        mByteBuffer1.order(ByteOrder.nativeOrder());
        this.mColorBuffer = mByteBuffer1.asFloatBuffer();
        this.mColorBuffer.position(0);
        ByteBuffer mByteBuffer2 = ByteBuffer.allocateDirect(148500);
        mByteBuffer2.order(ByteOrder.nativeOrder());
        this.mIndexBuffer = mByteBuffer2.asShortBuffer();
        this.mIndexBuffer.position(0);
        ByteBuffer mByteBuffer3 = ByteBuffer.allocateDirect(602400);
        mByteBuffer3.order(ByteOrder.nativeOrder());
        this.mGPSdata = mByteBuffer3.asDoubleBuffer();
        this.mGPSdata.position(0);
        short[] bg_index = {0, 1, 2, 2, 3, 0};
        float[] bg_texture = {0.0f, 1.0f, 0.0f, 0.4f, 1.0f, 0.4f, 1.0f, 1.0f};
        ByteBuffer mByteBufferB = ByteBuffer.allocateDirect(bg_texture.length * 4);
        mByteBufferB.order(ByteOrder.nativeOrder());
        this.mTB = mByteBufferB.asFloatBuffer();
        this.mTB.put(bg_texture);
        this.mTB.position(0);
        ByteBuffer mByteBufferC = ByteBuffer.allocateDirect(bg_index.length * 2);
        mByteBufferC.order(ByteOrder.nativeOrder());
        this.mIB = mByteBufferC.asShortBuffer();
        this.mIB.put(bg_index);
        this.mIB.position(0);
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glEnable(3553);
        int[] textures = new int[1];
        gl.glGenTextures(1, textures, 0);
        this.mTextureID = textures[0];
        gl.glBindTexture(3553, this.mTextureID);
        gl.glTexParameterf(3553, 10241, 9728.0f);
        gl.glTexParameterf(3553, 10240, 9729.0f);
        gl.glTexParameterf(3553, 10242, 33071.0f);
        gl.glTexParameterf(3553, 10243, 33071.0f);
        gl.glTexEnvf(8960, 8704, 7681.0f);
        InputStream is = this.mContext.getResources().openRawResource(R.raw.transform);
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            GLUtils.texImage2D(3553, 0, bitmap, 0);
            bitmap.recycle();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
            }
        }
    }

    public int ScreenWidth() {
        return this.ScreenWidth;
    }

    public int ScreenHeight() {
        return this.ScreenHeight;
    }

    private void RenderImage2D(GL10 gl, float pxX, float pxY, float pxWidth, float pxHeight, float texX, float texY, float texWidth, float texHeight, boolean Active) {
        float[] bg_vertex = {pxX, pxY, -0.5f, pxX, pxY + pxHeight, -0.5f, pxX + pxWidth, pxY + pxHeight, -0.5f, pxX + pxWidth, pxY, -0.5f};
        ByteBuffer mByteBufferA = ByteBuffer.allocateDirect(bg_vertex.length * 4);
        mByteBufferA.order(ByteOrder.nativeOrder());
        this.mVB = mByteBufferA.asFloatBuffer();
        this.mVB.put(bg_vertex);
        this.mVB.position(0);
        if (Active) {
            texX += 0.5f;
        }
        float[] bg_texture = {texX, texY + texHeight, texX, texY, texX + texWidth, texY, texX + texWidth, texY + texHeight};
        ByteBuffer mByteBufferB = ByteBuffer.allocateDirect(bg_texture.length * 4);
        mByteBufferB.order(ByteOrder.nativeOrder());
        this.mTB = mByteBufferB.asFloatBuffer();
        this.mTB.put(bg_texture);
        this.mTB.position(0);
        gl.glMatrixMode(5889);
        gl.glLoadIdentity();
        gl.glOrthof(0.0f, this.ScreenWidth, 0.0f, this.ScreenHeight, -1.0f, 1.0f);
        gl.glMatrixMode(5888);
        gl.glLoadIdentity();
        gl.glBindTexture(3553, this.mTextureID);
        gl.glEnable(3553);
        gl.glEnable(3042);
        gl.glDisable(2929);
        gl.glBlendFunc(770, 771);
        gl.glEnableClientState(32884);
        gl.glEnableClientState(32888);
        this.mVB.position(0);
        this.mTB.position(0);
        this.mIB.position(0);
        gl.glTexCoordPointer(2, 5126, 0, this.mTB);
        gl.glVertexPointer(3, 5126, 0, this.mVB);
        gl.glDrawElements(4, 6, 5123, this.mIB);
        gl.glDisableClientState(32888);
        gl.glDisableClientState(32884);
        gl.glDisable(3553);
        gl.glDisable(3042);
        gl.glEnable(2929);
    }

    public void setFieldLengthInMeters(int meters) {
        if (meters > 0 && meters < 250 / this.ImpulsesPerMeter) {
            this.FieldLengthInMeters = meters;
            this.FieldLengthInImpulses = (this.FieldLengthInMeters * this.ImpulsesPerMeter) + 1;
        }
    }

    public void setFieldLengthInImpulses(int impulses) {
        if (impulses > 0 && impulses < 251) {
            this.FieldLengthInImpulses = impulses;
            this.FieldLengthInMeters = (this.FieldLengthInImpulses - 1) / this.ImpulsesPerMeter;
        }
    }

    public void MetersToImpulses(short meters) {
        if (meters > 0) {
            this.Impulses = (short) ((meters * 6) + 1);
        } else {
            this.Impulses = (short) 10;
        }
    }

    public synchronized void setTransformationMode(byte theTransformationMode) {
        this.TransformationMode = theTransformationMode;
    }

    public synchronized void setScreenResolution(GL10 gl, int width, int height) {
        this.ScreenWidth = width;
        this.ScreenHeight = height;
        if (this.OperatingMode == 0 || this.OperatingMode == 2) {
            InitGrid();
        }
    }

    public synchronized void InitGrid() {
        this.PosX = (short) 0;
        this.PosY = (short) 0;
        this.IncY = (short) 1;
        this.mVertexBuffer.position(0);
        this.mColorBuffer.position(0);
        this.mIndexBuffer.position(0);
        this.VertexCount = 0;
        this.IndexCount = 0;
        this.Ymin = Float.MAX_VALUE;
        this.Ymax = Float.MIN_VALUE;
        if (this.OperatingMode == 1) {
            this.isFirstLine = true;
            this.isNextLine = false;
            this.IsScanning = true;
        } else {
            this.isFirstLine = false;
            this.isNextLine = false;
        }
        InitValue();
        switch (this.OperatingMode) {
            case 0:
                this.PrevValue = Float.NaN;
                this.MagValue = 0.0f;
                this.VertexCount = this.ScreenWidth / 5;
                this.IndexCount = this.VertexCount * 2;
                this.mVertexBuffer.position(0);
                this.mColorBuffer.position(0);
                this.mIndexBuffer.position(0);
                int MyInc = 0;
                for (int i = 0; i < this.VertexCount; i++) {
                    this.vertex[0] = MyInc;
                    this.vertex[1] = 0.0f;
                    this.vertex[2] = 0.0f;
                    this.mVertexBuffer.put(this.vertex);
                    this.colors[0] = 0.0f;
                    this.colors[1] = 1.0f;
                    this.colors[2] = 0.0f;
                    this.colors[3] = 1.0f - ((Math.abs((this.VertexCount / 2) - i) * 1.0f) / (this.VertexCount / 2));
                    this.mColorBuffer.put(this.colors);
                    this.vertex[0] = MyInc;
                    this.vertex[1] = 0.0f;
                    this.vertex[2] = 0.0f;
                    this.mVertexBuffer.put(this.vertex);
                    this.colors[0] = 0.0f;
                    this.colors[1] = 1.0f;
                    this.colors[2] = 0.0f;
                    this.mColorBuffer.put(this.colors);
                    this.mIndexBuffer.put((short) (i * 2));
                    this.mIndexBuffer.put((short) ((i * 2) + 1));
                    MyInc += 5;
                }
                this.Ymin = 0.0f;
                this.Ymax = 0.0f;
                break;
            case 2:
                this.PrevValue = Float.NaN;
                this.MagValue = 0.0f;
                this.VertexCount = this.ScreenWidth / 10;
                this.IndexCount = this.VertexCount;
                this.mVertexBuffer.position(0);
                this.mColorBuffer.position(0);
                this.mIndexBuffer.position(0);
                int MyInc2 = 0;
                for (int i2 = 0; i2 < this.VertexCount; i2++) {
                    this.vertex[0] = MyInc2;
                    this.vertex[1] = 0.0f;
                    this.vertex[2] = 0.0f;
                    this.mVertexBuffer.put(this.vertex);
                    this.colors[0] = 0.0f;
                    this.colors[1] = 1.0f;
                    this.colors[2] = 0.0f;
                    this.colors[3] = 1.0f;
                    this.mColorBuffer.put(this.colors);
                    this.mIndexBuffer.put((short) i2);
                    MyInc2 += 10;
                }
                this.Ymin = 0.0f;
                this.Ymax = 0.0f;
                break;
        }
        this.LocalX[0] = 1.0f;
        this.LocalX[1] = 0.0f;
        this.LocalX[2] = 0.0f;
        this.LocalY[0] = 0.0f;
        this.LocalY[1] = 1.0f;
        this.LocalY[2] = 0.0f;
        this.LocalZ[0] = 0.0f;
        this.LocalZ[1] = 0.0f;
        this.LocalZ[2] = 1.0f;
        this.ObjMat[0] = 1.0f;
        this.ObjMat[1] = 0.0f;
        this.ObjMat[2] = 0.0f;
        this.ObjMat[3] = 0.0f;
        this.ObjMat[4] = 0.0f;
        this.ObjMat[5] = 1.0f;
        this.ObjMat[6] = 0.0f;
        this.ObjMat[7] = 0.0f;
        this.ObjMat[8] = 0.0f;
        this.ObjMat[9] = 0.0f;
        this.ObjMat[10] = 1.0f;
        this.ObjMat[11] = 0.0f;
        this.ObjMat[12] = 0.0f;
        this.ObjMat[13] = 0.0f;
        this.ObjMat[14] = 0.0f;
        this.ObjMat[15] = 1.0f;
        this.RotationMatrix = GetIdentityMatrix();
    }

    public void isAutomatic(boolean automatic) {
        this.Automatic = automatic;
    }

    public boolean isAutomatic() {
        return this.Automatic;
    }

    public synchronized int AddValue(float value) {
        switch (this.OperatingMode) {
            case 0:
                Magnetometer_AddValue(value);
                break;
            case 2:
                Discrimination_AddValue(value);
                break;
        }
        return 0;
    }

    private int Magnetometer_GetValuePos(int index) {
        return (index * 6) + 4;
    }

    private int Magnetometer_GetColorPos(int index) {
        return (index * 8) + 4;
    }

    private void Magnetometer_AddValue(float CurrentValue) {
        int Pos = this.VertexCount / 2;
        this.Ymax = 0.0f;
        this.Ymin = 0.0f;
        for (int i = 0; i < Pos; i++) {
            int aPos = Magnetometer_GetValuePos(i + 1);
            int nPos = Magnetometer_GetValuePos(i);
            float nValue = this.mVertexBuffer.get(aPos) * 0.9f;
            this.mVertexBuffer.put(nPos, nValue);
            this.Ymin = Math.min(this.Ymin, nValue);
            this.Ymax = Math.max(this.Ymax, nValue);
            this.mColorBuffer.position(Magnetometer_GetColorPos(i + 1));
            this.mColorBuffer.get(this.colors);
            this.mColorBuffer.position(Magnetometer_GetColorPos(i));
            this.mColorBuffer.put(this.colors);
            int aPos2 = Magnetometer_GetValuePos((this.VertexCount - i) - 1);
            int nPos2 = Magnetometer_GetValuePos(this.VertexCount - i);
            float nValue2 = this.mVertexBuffer.get(aPos2) * 0.9f;
            this.mVertexBuffer.put(nPos2, nValue2);
            this.Ymin = Math.min(this.Ymin, nValue2);
            this.Ymax = Math.max(this.Ymax, nValue2);
            this.mColorBuffer.position(Magnetometer_GetColorPos((this.VertexCount - i) - 1));
            this.mColorBuffer.get(this.colors);
            this.mColorBuffer.position(Magnetometer_GetColorPos(this.VertexCount - i));
            this.mColorBuffer.put(this.colors);
        }
        if (Float.isNaN(CurrentValue)) {
            this.PrevValue = CurrentValue;
            this.MagValue = 0.0f;
            return;
        }
        if (Float.isNaN(this.PrevValue)) {
            this.PrevValue = CurrentValue;
            this.MagValue = 0.0f;
            this.Ymin = 0.0f;
            this.Ymax = 0.0f;
            return;
        }
        this.MagValue += CurrentValue - ((this.PrevValue + CurrentValue) / 2.0f);
        this.PrevValue = (this.PrevValue + CurrentValue) / 2.0f;
        float nValue3 = this.MagValue * 0.8f;
        this.Ymin = Math.min(this.Ymin, nValue3);
        this.Ymax = Math.max(this.Ymax, nValue3);
        createRGBA(this.colors, nValue3);
        if (nValue3 < 5.0f && nValue3 >= 0.0f) {
            nValue3 = 5.0f;
        } else if (nValue3 <= 0.0f && nValue3 > -5.0f) {
            nValue3 = -5.0f;
        }
        int nPos3 = Magnetometer_GetValuePos(Pos);
        this.mVertexBuffer.put(nPos3, nValue3);
        this.mColorBuffer.position(Magnetometer_GetColorPos(Pos));
        this.mColorBuffer.put(this.colors);
    }

    private void gpsWriteTo(int X, int Y, double longitude, double latitude, double quality) {
        int gpsPos = getVertexPos(X, Y);
        this.mGPSdata.position(gpsPos);
        this.mGPSdata.put(longitude);
        this.mGPSdata.put(latitude);
        this.mGPSdata.put(quality);
    }

    private int getVertexPos(int X, int Y) {
        return getVertexIdx(X, Y) * 3;
    }

    private int getVertexIdx(int X, int Y) {
        return (this.Impulses * X) + Y;
    }

    private void writeTo(int X, int Y, float[] vertex) {
        int VertexPos = getVertexPos(X, Y);
        this.mVertexBuffer.position(VertexPos);
        this.mVertexBuffer.put(vertex);
        this.VertexCount++;
    }

    private void InitValue() {
        this.Offset = 0.0f;
        this.Startwert = 0.0f;
        this.LastValue = 0.0f;
        this.DiffValue = 0.0f;
        this.isFirstValue = true;
        this.CalcNewDiff = false;
    }

    private float CalcValue(float Value) {
        if (this.IsScanning) {
            if (this.CalcNewDiff) {
                this.CalcNewDiff = false;
                this.DiffValue = ((this.ScanLineMedian + this.LastValue) / 2.0f) - Value;
                this.ScanLineMedian = Value;
            }
            if (this.isFirstValue) {
                this.isFirstValue = false;
                this.ScanLineMedian = Value;
                this.LastValue = Value;
            } else {
                this.LastValue = (this.LastValue + Value) / 2.0f;
            }
            this.ScanLineMedian = (this.ScanLineMedian + Value) / 2.0f;
            return this.LastValue + Value + this.DiffValue;
        }
        return Value;
    }

    public synchronized int GroundScan_AddValue(float value, double longitude, double latitude, double quality) {
        int returnValue;
        returnValue = this.PosY == 0 ? 0 : this.PosY % 6;
        this.f_canSave = this.PosX > 0;
        if (this.PosY < this.Impulses && this.PosX < 100 && this.PosY < 251) {
            this.isFirstLine = false;
            this.isNextLine = false;
            this.vertex[0] = -this.PosX;
            this.vertex[1] = this.PosY;
            this.vertex[2] = CalcValue(value);
            if (this.IsScanning && this.PosX > 0) {
                float tmp = this.mVertexBuffer.get(getVertexPos(this.PosX - 1, this.PosY) + 2);
                this.vertex[2] = (this.vertex[2] + tmp) / 2.0f;
            }
            writeTo(this.PosX, this.PosY, this.vertex);
            gpsWriteTo(this.PosX, this.PosY, longitude, latitude, quality);
            this.Ymax = Math.max(this.Ymax, this.vertex[2]);
            this.Ymin = Math.min(this.Ymin, this.vertex[2]);
            if (this.PosX == 0) {
                this.vertex[0] = -(this.PosX + 0.5f);
                this.vertex[1] = this.PosY;
                writeTo(this.PosX + 1, this.PosY, this.vertex);
                this.VertexCount--;
            }
            if (this.PosY > 0) {
                if (this.PosX == 0) {
                    this.mIndexBuffer.put((short) getVertexIdx(this.PosX + 1, this.PosY));
                    this.mIndexBuffer.put((short) getVertexIdx(this.PosX + 1, this.PosY - 1));
                    this.mIndexBuffer.put((short) getVertexIdx((this.PosX - 1) + 1, this.PosY - 1));
                    this.mIndexBuffer.put((short) getVertexIdx(this.PosX + 1, this.PosY));
                    this.mIndexBuffer.put((short) getVertexIdx((this.PosX - 1) + 1, this.PosY - 1));
                    this.mIndexBuffer.put((short) getVertexIdx((this.PosX - 1) + 1, this.PosY));
                    this.IndexCount += 6;
                } else if (this.PosX > 1) {
                    this.mIndexBuffer.put((short) getVertexIdx(this.PosX, this.PosY));
                    this.mIndexBuffer.put((short) getVertexIdx(this.PosX, this.PosY - 1));
                    this.mIndexBuffer.put((short) getVertexIdx(this.PosX - 1, this.PosY - 1));
                    this.mIndexBuffer.put((short) getVertexIdx(this.PosX, this.PosY));
                    this.mIndexBuffer.put((short) getVertexIdx(this.PosX - 1, this.PosY - 1));
                    this.mIndexBuffer.put((short) getVertexIdx(this.PosX - 1, this.PosY));
                    this.IndexCount += 6;
                }
            }
            if ((this.IncY > 0 && this.PosY < this.Impulses - 1) || (this.IncY < 0 && this.PosY > 0 && this.PosY <= this.Impulses - 1)) {
                if (this.IncY < 0) {
                    this.PosY = (short) (this.PosY + this.IncY);
                } else if (this.IncY > 0) {
                    this.PosY = (short) (this.PosY + this.IncY);
                }
            } else {
                this.isNextLine = true;
                this.CalcNewDiff = true;
                if (!this.ZigZag) {
                    this.PosY = (short) 0;
                    this.IncY = (short) 1;
                } else {
                    this.IncY = (short) (-this.IncY);
                }
                this.PosX = (short) (this.PosX + 1);
            }
        }
        return returnValue;
    }

    private int Discrimination_GetValuePos(int index) {
        return (index * 3) + 1;
    }

    private int Discrimination_GetColorPos(int index) {
        return index * 4;
    }

    private void Discrimination_AddValue(float CurrentValue) {
        this.Ymax = 0.0f;
        this.Ymin = 0.0f;
        for (int i = 0; i < this.VertexCount; i++) {
            float nValue = this.mVertexBuffer.get(Discrimination_GetValuePos(i + 1));
            this.mVertexBuffer.put(Discrimination_GetValuePos(i), nValue);
            this.Ymin = Math.min(this.Ymin, nValue);
            this.Ymax = Math.max(this.Ymax, nValue);
            this.mColorBuffer.position(Discrimination_GetColorPos(i + 1));
            this.mColorBuffer.get(this.colors);
            this.mColorBuffer.position(Discrimination_GetColorPos(i));
            this.mColorBuffer.put(this.colors);
        }
        if (Float.isNaN(CurrentValue)) {
            InitGrid();
            return;
        }
        if (Float.isNaN(this.PrevValue)) {
            this.PrevValue = CurrentValue;
            this.MagValue = 0.0f;
            this.Ymin = 0.0f;
            this.Ymax = 0.0f;
            return;
        }
        this.MagValue += CurrentValue - this.PrevValue;
        this.PrevValue = CurrentValue;
        float nValue2 = this.MagValue;
        this.Ymin = Math.min(this.Ymin, nValue2);
        this.Ymax = Math.max(this.Ymax, nValue2);
        createRGBA(this.colors, nValue2);
        if (nValue2 < 5.0f && nValue2 >= 0.0f) {
            nValue2 = 5.0f;
        } else if (nValue2 <= 0.0f && nValue2 > -5.0f) {
            nValue2 = -5.0f;
        }
        int nPos = Discrimination_GetValuePos(this.VertexCount);
        this.mVertexBuffer.put(nPos, nValue2);
        this.mColorBuffer.position(Discrimination_GetColorPos(this.VertexCount));
        this.mColorBuffer.put(this.colors);
    }

    public synchronized void getVertexBuffer(FloatBuffer aVertexBuffer, FloatBuffer aColorBuffer) {
        int OldPos = this.mVertexBuffer.position();
        if (this.mColorBuffer != null) {
            if (this.OperatingMode == 2) {
                this.Ymin = Float.MAX_VALUE;
                this.Ymax = Float.MIN_VALUE;
            }
            this.mVertexBuffer.position(0);
            this.mColorBuffer.position(0);
            int myCount = this.VertexCount + this.Impulses;
            for (int i = 0; i < myCount; i++) {
                this.mVertexBuffer.get(this.vertex);
                createRGBA(this.colors, this.vertex[2]);
                this.mColorBuffer.put(this.colors);
            }
            this.mVertexBuffer.position(OldPos);
        }
    }

    private synchronized void createRGBA(float[] RGBA, float f) {
        RGBA[3] = 1.0f;
        RGBA[2] = 1.0f;
        RGBA[1] = 1.0f;
        RGBA[0] = 1.0f;
        float A = (this.Ymax + this.Ymin) / 2.0f;
        if (f >= this.Ymax) {
            RGBA[0] = 1.0f;
            RGBA[1] = 0.0f;
            RGBA[2] = 0.0f;
        } else if (f <= this.Ymin) {
            RGBA[0] = 0.0f;
            RGBA[1] = 0.0f;
            RGBA[2] = 1.0f;
        } else if (f == A) {
            RGBA[0] = 0.0f;
            RGBA[1] = 1.0f;
            RGBA[2] = 0.0f;
        } else if (f > A) {
            RGBA[0] = ((f - A) / (this.Ymax - A)) * 0.9f;
            RGBA[1] = 1.0f - RGBA[0];
            RGBA[2] = 0.0f;
        } else if (f < A) {
            RGBA[0] = 0.0f;
            RGBA[1] = 1.0f - (((f - this.Ymin) / (A - this.Ymin)) * 0.9f);
            RGBA[2] = 1.0f - RGBA[1];
        }
    }

    private float MakeRGB(int value) {
        float tmp = value / 255.0f;
        if (tmp < 0.0f) {
            return 0.0f;
        }
        if (tmp > 1.0f) {
            return 1.0f;
        }
        return tmp;
    }

    private synchronized void createRGBA_Ext(float[] RGBA, float f) {
        RGBA[3] = 1.0f;
        RGBA[2] = 0.0f;
        RGBA[1] = 0.0f;
        RGBA[0] = 1.0f;
        float fp = ((f - this.Ymin) * 95.0f) / (this.Ymax - this.Ymin);
        if (fp > 95.0f) {
            RGBA[2] = MakeRGB(0);
            RGBA[1] = MakeRGB(0);
            RGBA[0] = MakeRGB(255);
        } else if (fp > 90.0f) {
            RGBA[2] = MakeRGB(0);
            RGBA[1] = MakeRGB(77);
            RGBA[0] = MakeRGB(255);
        } else if (fp > 85.0f) {
            RGBA[2] = MakeRGB(0);
            RGBA[1] = MakeRGB(135);
            RGBA[0] = MakeRGB(255);
        } else if (fp > 80.0f) {
            RGBA[2] = MakeRGB(0);
            RGBA[1] = MakeRGB(166);
            RGBA[0] = MakeRGB(255);
        } else if (fp > 75.0f) {
            RGBA[2] = MakeRGB(0);
            RGBA[1] = MakeRGB(230);
            RGBA[0] = MakeRGB(255);
        } else if (fp > 70.0f) {
            RGBA[2] = MakeRGB(0);
            RGBA[1] = MakeRGB(230);
            RGBA[0] = MakeRGB(0);
        } else if (fp > 65.0f) {
            RGBA[2] = MakeRGB(0);
            RGBA[1] = MakeRGB(179);
            RGBA[0] = MakeRGB(0);
        } else if (fp > 60.0f) {
            RGBA[2] = MakeRGB(0);
            RGBA[1] = MakeRGB(166);
            RGBA[0] = MakeRGB(0);
        } else if (fp > 55.0f) {
            RGBA[2] = MakeRGB(0);
            RGBA[1] = MakeRGB(153);
            RGBA[0] = MakeRGB(0);
        } else if (fp > 50.0f) {
            RGBA[2] = MakeRGB(0);
            RGBA[1] = MakeRGB(140);
            RGBA[0] = MakeRGB(0);
        } else if (fp > 45.0f) {
            RGBA[2] = MakeRGB(51);
            RGBA[1] = MakeRGB(140);
            RGBA[0] = MakeRGB(0);
        } else if (fp > 40.0f) {
            RGBA[2] = MakeRGB(89);
            RGBA[1] = MakeRGB(140);
            RGBA[0] = MakeRGB(0);
        } else if (fp > 35.0f) {
            RGBA[2] = MakeRGB(204);
            RGBA[1] = MakeRGB(165);
            RGBA[0] = MakeRGB(0);
        } else if (fp > 30.0f) {
            RGBA[2] = MakeRGB(242);
            RGBA[1] = MakeRGB(217);
            RGBA[0] = MakeRGB(0);
        } else if (fp > 25.0f) {
            RGBA[2] = MakeRGB(242);
            RGBA[1] = MakeRGB(153);
            RGBA[0] = MakeRGB(0);
        } else if (fp > 20.0f) {
            RGBA[2] = MakeRGB(230);
            RGBA[1] = MakeRGB(77);
            RGBA[0] = MakeRGB(0);
        } else if (fp > 15.0f) {
            RGBA[2] = MakeRGB(255);
            RGBA[1] = MakeRGB(0);
            RGBA[0] = MakeRGB(0);
        } else if (fp > 10.0f) {
            RGBA[2] = MakeRGB(204);
            RGBA[1] = MakeRGB(0);
            RGBA[0] = MakeRGB(0);
        } else if (fp > 5.0f) {
            RGBA[2] = MakeRGB(153);
            RGBA[1] = MakeRGB(0);
            RGBA[0] = MakeRGB(0);
        } else {
            RGBA[2] = MakeRGB(102);
            RGBA[1] = MakeRGB(0);
            RGBA[0] = MakeRGB(0);
        }
    }

    private void Transform(GL10 gl) {
        float X = this.PosX / 2;
        float Y = this.Impulses / 2;
        float Z = (this.Ymax + this.Ymin) / 2.0f;
        gl.glTranslatef(-X, Y, Z);
        gl.glTranslatef(this.translateX, this.translateY, this.translateZ);
        gl.glMultMatrixf(this.RotationMatrix, 0);
        if (this.Ymax - this.Ymin > Math.min(this.bRight - this.bLeft, this.bTop - this.bBottom)) {
            this.scaleZ = Math.min(this.bRight - this.bLeft, this.bTop - this.bBottom) / (this.Ymax - this.Ymin);
            gl.glScalef(1.0f, 1.0f, this.scaleZ);
        }
        gl.glTranslatef(X, -Y, -Z);
    }

    public synchronized void Render(GL10 gl) {
        switch (this.OperatingMode) {
            case 0:
                Magnetometer_Render(gl);
                break;
            case 1:
                GroundScan_Render(gl);
                break;
            case 2:
                Discrimination_Render(gl);
                break;
        }
    }

    private void Magnetometer_Render(GL10 gl) {
        gl.glMatrixMode(5889);
        gl.glLoadIdentity();
        int tmp = Math.max(Math.abs((int) Math.max(Math.abs(this.Ymax), Math.abs(this.Ymin))), Math.abs(this.ScreenHeight));
        gl.glOrthof(0.0f, this.ScreenWidth, -tmp, tmp, -1.0f, 1.0f);
        gl.glMatrixMode(5888);
        gl.glLoadIdentity();
        gl.glDisable(3553);
        gl.glEnable(3042);
        gl.glBlendFunc(770, 771);
        gl.glLineWidth(2.0f);
        gl.glEnableClientState(32884);
        gl.glEnableClientState(32886);
        this.mVertexBuffer.position(0);
        this.mColorBuffer.position(0);
        this.mIndexBuffer.position(0);
        gl.glColorPointer(4, 5126, 0, this.mColorBuffer);
        gl.glVertexPointer(3, 5126, 0, this.mVertexBuffer);
        gl.glDrawElements(1, this.IndexCount, 5123, this.mIndexBuffer);
        gl.glDisableClientState(32886);
        gl.glDisableClientState(32884);
    }

    private synchronized void GroundScan_Render(GL10 gl) {
        this.bLeft = -Math.max((int) this.PosX, 10);
        this.bRight = 0.0f;
        this.bTop = this.Impulses - 1;
        this.bBottom = 0.0f;
        this.bNear = -(this.Ymin - 10.0f);
        this.bFar = -(this.Ymax + 10.0f);
        gl.glMatrixMode(5889);
        gl.glLoadIdentity();
        gl.glOrthof(this.bLeft - this.zoom, this.zoom + this.bRight, this.bBottom - this.zoom, this.zoom + this.bTop, this.bNear, this.bFar);
        gl.glMatrixMode(5888);
        gl.glLoadIdentity();
        gl.glDisable(3553);
        gl.glDisable(3042);
        int OldPos = this.mVertexBuffer.position();
        this.mVertexBuffer.position(0);
        this.mColorBuffer.position(0);
        int myCount = this.VertexCount + this.Impulses;
        for (int i = 0; i < myCount; i++) {
            this.mVertexBuffer.get(this.vertex);
            createRGBA_Ext(this.colors, this.vertex[2]);
            this.mColorBuffer.put(this.colors);
        }
        this.mVertexBuffer.position(OldPos);
        int VertexBufferPos = this.mVertexBuffer.position();
        int ColorBufferPos = this.mColorBuffer.position();
        int IndexBufferPos = this.mIndexBuffer.position();
        if (!this.ScanActive) {
            Transform(gl);
        }
        gl.glLineWidth(2.0f);
        gl.glEnableClientState(32884);
        gl.glEnableClientState(32886);
        this.mVertexBuffer.position(0);
        this.mColorBuffer.position(0);
        this.mIndexBuffer.position(0);
        gl.glColorPointer(4, 5126, 0, this.mColorBuffer);
        gl.glVertexPointer(3, 5126, 0, this.mVertexBuffer);
        gl.glDrawElements(4, this.IndexCount, 5123, this.mIndexBuffer);
        this.mVertexBuffer.position(VertexBufferPos);
        this.mColorBuffer.position(ColorBufferPos);
        this.mIndexBuffer.position(IndexBufferPos);
        gl.glDisableClientState(32886);
        gl.glDisableClientState(32884);
        if (!this.ScanActive) {
            if (this.ScreenWidth < this.ScreenHeight) {
                float dist = this.ScreenWidth / 5;
                RenderImage2D(gl, (1.0f * dist) - 32.0f, 5.0f, 64.0f, 64.0f, 0.0f, 0.0f, 0.25f, 0.25f, this.TransformationMode == 0);
                RenderImage2D(gl, (2.0f * dist) - 32.0f, 5.0f, 64.0f, 64.0f, 0.25f, 0.0f, 0.25f, 0.25f, this.TransformationMode == 1 || this.TransformationMode == 2 || this.TransformationMode == 3);
                RenderImage2D(gl, (3.0f * dist) - 32.0f, 5.0f, 64.0f, 64.0f, 0.0f, 0.25f, 0.25f, 0.25f, this.TransformationMode == 4);
                RenderImage2D(gl, (4.0f * dist) - 32.0f, 5.0f, 64.0f, 64.0f, 0.25f, 0.25f, 0.25f, 0.25f, false);
            } else {
                float dist2 = this.ScreenHeight / 5;
                RenderImage2D(gl, 5.0f, (1.0f * dist2) - 32.0f, 64.0f, 64.0f, 0.0f, 0.0f, 0.25f, 0.25f, this.TransformationMode == 0);
                RenderImage2D(gl, 5.0f, (2.0f * dist2) - 32.0f, 64.0f, 64.0f, 0.25f, 0.0f, 0.25f, 0.25f, this.TransformationMode == 1 || this.TransformationMode == 2 || this.TransformationMode == 3);
                RenderImage2D(gl, 5.0f, (3.0f * dist2) - 32.0f, 64.0f, 64.0f, 0.0f, 0.25f, 0.25f, 0.25f, this.TransformationMode == 4);
                RenderImage2D(gl, 5.0f, (4.0f * dist2) - 32.0f, 64.0f, 64.0f, 0.25f, 0.25f, 0.25f, 0.25f, false);
            }
        }
    }

    private void Discrimination_Render(GL10 gl) {
        gl.glMatrixMode(5889);
        gl.glLoadIdentity();
        int tmp = Math.max(Math.abs((int) Math.max(Math.abs(this.Ymax), Math.abs(this.Ymin))), Math.abs(this.ScreenHeight));
        gl.glOrthof(0.0f, this.ScreenWidth, -tmp, tmp, -1.0f, 1.0f);
        gl.glMatrixMode(5888);
        gl.glLoadIdentity();
        gl.glDisable(3553);
        gl.glLineWidth(2.0f);
        gl.glEnableClientState(32884);
        gl.glEnableClientState(32886);
        this.mVertexBuffer.position(0);
        this.mColorBuffer.position(0);
        this.mIndexBuffer.position(0);
        gl.glColorPointer(4, 5126, 0, this.mColorBuffer);
        gl.glVertexPointer(3, 5126, 0, this.mVertexBuffer);
        gl.glDrawElements(3, this.VertexCount, 5123, this.mIndexBuffer);
        gl.glDisableClientState(32886);
        gl.glDisableClientState(32884);
    }

    private void writeDelphiString(DataOutputStream stream, String string, int MaxLen) throws IOException {
        stream.writeByte(string.length());
        stream.writeBytes(string);
        for (int i = string.length(); i < MaxLen; i++) {
            stream.writeByte(0);
        }
    }

    private void writeDelphiFloat(DataOutputStream stream, float value) throws IOException {
        stream.writeInt(Integer.reverseBytes(Float.floatToIntBits(value)));
    }

    public synchronized boolean FileExists(String aFileName) {
        boolean z;
        String aFileName2 = aFileName.trim();
        if (!aFileName2.endsWith(".v3d")) {
            aFileName2 = aFileName2.concat(".v3d");
        }
        String state = Environment.getExternalStorageState();
        if (aFileName2.length() > 0 && aFileName2.endsWith(".v3d") && "mounted".equals(state)) {
            File MyFile = Environment.getExternalStorageDirectory();
            if (MyFile.exists() && MyFile.canWrite()) {
                String path = "/sd/OKM/" + aFileName2;
                z = new File(MyFile, path).exists();
            }
        }
        z = false;
        return z;
    }

    public synchronized boolean SaveToFile(String aFileName) {
        boolean z;
        float x;
        float y;
        float value;
        double longitude;
        double latitude;
        double quality;
        String aFileName2 = aFileName.trim();
        if (!aFileName2.endsWith(".v3d")) {
            aFileName2 = aFileName2.concat(".v3d");
        }
        String state = Environment.getExternalStorageState();
        if (aFileName2.length() > 0 && aFileName2.endsWith(".v3d") && "mounted".equals(state)) {
            File MyFile = Environment.getExternalStorageDirectory();
            if (MyFile.exists() && MyFile.canWrite()) {
                File MyFile2 = new File(MyFile, "/sd/OKM/");
                if (!MyFile2.exists()) {
                    MyFile2.mkdirs();
                }
                try {
                    DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(String.valueOf(MyFile2.getAbsolutePath()) + "/" + aFileName2)));
                    writeDelphiString(stream, "V3D.V3D.01", 10);
                    writeDelphiString(stream, "V3D.V3D.01", 10);
                    writeDelphiString(stream, "(c)2011 OKM GmbH", 50);
                    writeDelphiString(stream, "OKM GmbH", 30);
                    writeDelphiString(stream, "OKM Rover 007", 20);
                    writeDelphiString(stream, "", 15);
                    writeDelphiString(stream, "", 25);
                    stream.writeByte(0);
                    stream.writeByte(0);
                    stream.writeByte(0);
                    stream.writeByte(0);
                    stream.writeBoolean(false);
                    if (this.Automatic) {
                        stream.writeBoolean(true);
                    } else {
                        stream.writeBoolean(false);
                    }
                    stream.writeByte(0);
                    stream.writeByte(0);
                    stream.writeInt(Integer.reverseBytes(this.Impulses));
                    stream.writeInt(Integer.reverseBytes(1));
                    stream.writeByte(0);
                    stream.writeByte(0);
                    stream.writeByte(0);
                    stream.writeByte(0);
                    byte[] Transformation = new byte[192];
                    Transformation[34] = Byte.MIN_VALUE;
                    Transformation[35] = 63;
                    Transformation[38] = Byte.MIN_VALUE;
                    Transformation[39] = 63;
                    Transformation[42] = Byte.MIN_VALUE;
                    Transformation[43] = 63;
                    Transformation[82] = Byte.MIN_VALUE;
                    Transformation[83] = 63;
                    Transformation[86] = Byte.MIN_VALUE;
                    Transformation[87] = 63;
                    Transformation[90] = Byte.MIN_VALUE;
                    Transformation[91] = 63;
                    Transformation[114] = -76;
                    Transformation[115] = -62;
                    Transformation[130] = Byte.MIN_VALUE;
                    Transformation[131] = 63;
                    Transformation[134] = Byte.MIN_VALUE;
                    Transformation[135] = 63;
                    Transformation[138] = Byte.MIN_VALUE;
                    Transformation[139] = 63;
                    Transformation[162] = 52;
                    Transformation[163] = -62;
                    Transformation[170] = 52;
                    Transformation[171] = -62;
                    Transformation[178] = Byte.MIN_VALUE;
                    Transformation[179] = 63;
                    Transformation[182] = Byte.MIN_VALUE;
                    Transformation[183] = 63;
                    Transformation[186] = Byte.MIN_VALUE;
                    Transformation[187] = 63;
                    for (byte b : Transformation) {
                        stream.writeByte(b);
                    }
                    stream.writeDouble(0.0d);
                    stream.writeInt(0);
                    stream.writeInt(Integer.reverseBytes(16));
                    stream.writeBytes("D\u0000e\u0000v\u0000i\u0000c\u0000e\u0000:\u0000 \u0000R\u0000o\u0000v\u0000e\u0000r\u0000 \u0000U\u0000C\u0000");
                    stream.writeFloat(0.0f);
                    stream.writeFloat(0.0f);
                    stream.writeInt(0);
                    for (int i = 0; i < this.VertexCount; i++) {
                        int Px = i / this.Impulses;
                        int Py = i % this.Impulses;
                        if (Px % 2 != 0) {
                            int tmp = (this.Impulses * Px) + ((this.Impulses - Py) - 1);
                            x = this.mVertexBuffer.get(tmp * 3);
                            y = this.mVertexBuffer.get((tmp * 3) + 1);
                            value = this.mVertexBuffer.get((tmp * 3) + 2);
                            longitude = this.mGPSdata.get(tmp * 3);
                            latitude = this.mGPSdata.get((tmp * 3) + 1);
                            quality = this.mGPSdata.get((tmp * 3) + 2);
                        } else {
                            x = this.mVertexBuffer.get(i * 3);
                            y = this.mVertexBuffer.get((i * 3) + 1);
                            value = this.mVertexBuffer.get((i * 3) + 2);
                            longitude = this.mGPSdata.get(i * 3);
                            latitude = this.mGPSdata.get((i * 3) + 1);
                            quality = this.mGPSdata.get((i * 3) + 2);
                        }
                        writeDelphiFloat(stream, value);
                        writeDelphiFloat(stream, x);
                        writeDelphiFloat(stream, y);
                        stream.writeLong(Long.reverseBytes(Double.doubleToLongBits(longitude)));
                        stream.writeLong(Long.reverseBytes(Double.doubleToLongBits(latitude)));
                        stream.writeLong(Long.reverseBytes(Double.doubleToLongBits(quality)));
                    }
                    stream.close();
                    z = true;
                } catch (FileNotFoundException e) {
                    z = false;
                } catch (IOException e2) {
                    z = false;
                }
            }
        }
        z = false;
        return z;
    }

    public synchronized boolean OpenFromFile(String aFileName) {
        boolean z;
        String state = Environment.getExternalStorageState();
        if (aFileName.length() > 0 && ("mounted".equals(state) || "mounted_ro".equals(state))) {
            File MyFile = Environment.getExternalStorageDirectory();
            if (MyFile.exists() && MyFile.canRead()) {
                boolean exists = new File(MyFile, "/sd/OKM/").exists();
                if (exists) {
                    try {
                        String path = String.valueOf(MyFile.getPath()) + "/sd/OKM/" + aFileName;
                        DataInputStream stream = new DataInputStream(new FileInputStream(path));
                        stream.skipBytes(172);
                        this.Automatic = stream.readBoolean();
                        if (stream.readByte() == 0) {
                            this.ZigZag = true;
                        } else {
                            this.ZigZag = false;
                        }
                        stream.skipBytes(1);
                        this.Impulses = (short) Integer.reverseBytes(stream.readInt());
                        stream.skipBytes(260);
                        InitGrid();
                        this.IsScanning = false;
                        while (stream.available() > 0) {
                            float tmp = Float.intBitsToFloat(Integer.reverseBytes(stream.readInt()));
                            stream.skipBytes(8);
                            double longitude = Double.longBitsToDouble(Long.reverseBytes(stream.readLong()));
                            double latitude = Double.longBitsToDouble(Long.reverseBytes(stream.readLong()));
                            double quality = Double.longBitsToDouble(Long.reverseBytes(stream.readLong()));
                            GroundScan_AddValue((int) tmp, longitude, latitude, quality);
                        }
                        stream.close();
                        z = true;
                    } catch (FileNotFoundException e) {
                        z = false;
                    } catch (IOException e2) {
                        z = false;
                    }
                } else {
                    z = false;
                }
            }
        }
        z = false;
        return z;
    }

    public synchronized void ResetTransformation() {
        this.translateX = 0.0f;
        this.translateY = 0.0f;
        this.translateZ = 0.0f;
        this.angleX = 0.0f;
        this.angleY = 0.0f;
        this.angleZ = 0.0f;
        this.scaleX = 1.0f;
        this.scaleY = 1.0f;
        this.scaleZ = Math.min(this.ScreenWidth, this.ScreenHeight) / (this.Ymax - this.Ymin);
        this.zoom = 0.0f;
        this.RotationMatrix = GetIdentityMatrix();
        this.CurrentView = (byte) 1;
    }

    public synchronized void setTopView() {
        this.translateX = 0.0f;
        this.translateY = 0.0f;
        this.translateZ = 0.0f;
        this.zoom = 0.0f;
        this.angleX = 0.0f;
        this.angleY = 0.0f;
        this.angleZ = 0.0f;
        this.RotationMatrix = GetIdentityMatrix();
        this.CurrentView = (byte) 1;
    }

    public synchronized void setSideView() {
        this.translateX = 0.0f;
        this.translateY = 0.0f;
        this.translateZ = 0.0f;
        this.zoom = 0.0f;
        this.angleX = 90.0f;
        this.angleY = 0.0f;
        this.angleZ = 0.0f;
        this.RotationMatrix = GetIdentityMatrix();
        this.RotationMatrix = MatrixMultiply(CreateRotationMatrix(this.LocalX, this.angleX), this.RotationMatrix);
        this.CurrentView = (byte) 2;
    }

    public synchronized void setPerspectiveView() {
        this.translateX = 0.0f;
        this.translateY = 0.0f;
        this.translateZ = 0.0f;
        this.zoom = 15.0f;
        this.angleX = 40.0f;
        this.angleY = 15.0f;
        this.angleZ = 0.0f;
        this.RotationMatrix = GetIdentityMatrix();
        this.RotationMatrix = MatrixMultiply(CreateRotationMatrix(this.LocalX, this.angleX), this.RotationMatrix);
        this.RotationMatrix = MatrixMultiply(CreateRotationMatrix(this.LocalY, this.angleY), this.RotationMatrix);
        this.CurrentView = (byte) 3;
    }

    public synchronized void SwitchView() {
        switch (this.CurrentView) {
            case 1:
                setSideView();
                break;
            case 2:
                setPerspectiveView();
                break;
            case 3:
                setTopView();
                break;
            default:
                setTopView();
                break;
        }
    }

    public synchronized void TransformBy(float dx, float dy, int TouchMode) {
        switch (this.TransformationMode) {
            case 0:
                this.translateX -= ((this.bLeft - (this.zoom * 2.0f)) * dx) / this.ScreenWidth;
                this.translateY -= ((this.bTop + (this.zoom * 2.0f)) * dy) / this.ScreenHeight;
                this.translateZ = 0.0f;
                break;
            case 1:
                if (TouchMode == 4) {
                    this.angleY = (180.0f * dx) / this.ScreenWidth;
                    this.angleX = (180.0f * dy) / this.ScreenHeight;
                    this.RotationMatrix = MatrixMultiply(CreateRotationMatrix(this.LocalX, this.angleX), this.RotationMatrix);
                    this.RotationMatrix = MatrixMultiply(CreateRotationMatrix(this.LocalY, this.angleY), this.RotationMatrix);
                    break;
                } else if (TouchMode == 0) {
                    this.angleZ = ((-180.0f) * dy) / this.ScreenHeight;
                    this.RotationMatrix = MatrixMultiply(CreateRotationMatrix(this.LocalZ, this.angleZ), this.RotationMatrix);
                    break;
                } else if (TouchMode == 2) {
                    this.angleZ = (180.0f * dy) / this.ScreenHeight;
                    this.RotationMatrix = MatrixMultiply(CreateRotationMatrix(this.LocalZ, this.angleZ), this.RotationMatrix);
                    break;
                } else if (TouchMode == 1) {
                    this.angleZ = (180.0f * dx) / this.ScreenWidth;
                    this.RotationMatrix = MatrixMultiply(CreateRotationMatrix(this.LocalZ, this.angleZ), this.RotationMatrix);
                    break;
                } else if (TouchMode == 3) {
                    this.angleZ = ((-180.0f) * dx) / this.ScreenWidth;
                    this.RotationMatrix = MatrixMultiply(CreateRotationMatrix(this.LocalZ, this.angleZ), this.RotationMatrix);
                    break;
                }
                break;
            case 4:
                float oldZoom = this.zoom;
                this.zoom -= (dx + dy) / 6.0f;
                if (this.bRight + this.zoom <= this.bLeft - this.zoom || this.bBottom - this.zoom >= this.bTop + this.zoom) {
                    this.zoom = oldZoom;
                    break;
                }
                break;
        }
        this.CurrentView = (byte) 0;
    }

    public void setImpulses(int value) {
        this.Impulses = (short) value;
    }

    public void setZigZag(boolean value) {
        this.ZigZag = value;
    }

    public void UseMagSensor(boolean MagSensor) {
        this.UseAndroidMagSensor = MagSensor;
    }

    public int UserActionRequired() {
        if (this.isFirstLine || this.isNextLine) {
            return this.PosX;
        }
        return -1;
    }

    public boolean canSave() {
        return this.f_canSave;
    }

    public void Continue() {
        this.isFirstLine = false;
        this.isNextLine = false;
    }

    private float VectorNormalize(float[] V) {
        return (float) Math.sqrt((V[0] * V[0]) + (V[1] * V[1]) + (V[2] * V[2]));
    }

    private int GetMatrixIdx(int Row, int Col) {
        return (Col * 4) + Row;
    }

    private float[] MatrixMultiply(float[] M1, float[] M2) {
        float[] Result = new float[16];
        for (int Row = 0; Row < 4; Row++) {
            for (int Col = 0; Col < 4; Col++) {
                Result[GetMatrixIdx(Row, Col)] = (M1[GetMatrixIdx(Row, 0)] * M2[GetMatrixIdx(0, Col)]) + (M1[GetMatrixIdx(Row, 1)] * M2[GetMatrixIdx(1, Col)]) + (M1[GetMatrixIdx(Row, 2)] * M2[GetMatrixIdx(2, Col)]) + (M1[GetMatrixIdx(Row, 3)] * M2[GetMatrixIdx(3, Col)]);
            }
        }
        return Result;
    }

    private float[] GetIdentityMatrix() {
        float[] Result = {1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f};
        return Result;
    }

    private float Sqr(float value) {
        return value * value;
    }

    private float[] CreateRotationMatrix(float[] Axis, float Angle) {
        float[] Result = new float[16];
        float Sine = (float) Math.sin(Math.toRadians(Angle));
        float Cosine = (float) Math.cos(Math.toRadians(Angle));
        float one_minus_cosine = 1.0f - Cosine;
        float Len = VectorNormalize(Axis);
        if (Len == 0.0f) {
            return GetIdentityMatrix();
        }
        Result[0] = (Sqr(Axis[0]) * one_minus_cosine) + Cosine;
        Result[1] = ((Axis[0] * one_minus_cosine) * Axis[1]) - (Axis[2] * Sine);
        Result[2] = (Axis[2] * one_minus_cosine * Axis[0]) + (Axis[1] * Sine);
        Result[3] = 0.0f;
        Result[4] = (Axis[0] * one_minus_cosine * Axis[1]) + (Axis[2] * Sine);
        Result[5] = (Sqr(Axis[1]) * one_minus_cosine) + Cosine;
        Result[6] = ((Axis[1] * one_minus_cosine) * Axis[2]) - (Axis[0] * Sine);
        Result[7] = 0.0f;
        Result[8] = ((Axis[2] * one_minus_cosine) * Axis[0]) - (Axis[1] * Sine);
        Result[9] = (Axis[1] * one_minus_cosine * Axis[2]) + (Axis[0] * Sine);
        Result[10] = (Sqr(Axis[2]) * one_minus_cosine) + Cosine;
        Result[11] = 0.0f;
        Result[12] = 0.0f;
        Result[13] = 0.0f;
        Result[14] = 0.0f;
        Result[15] = 1.0f;
        return Result;
    }
}
