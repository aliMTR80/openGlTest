package com.okm.roveruc;

import android.opengl.GLSurfaceView;
import android.view.Menu;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/* loaded from: classes.dex */
class ScanRenderer3D implements GLSurfaceView.Renderer {
    private float mAngle;
    ScanDataObject mScanDataObject;

    ScanRenderer3D() {
    }

    public void setScanDataObject(ScanDataObject mObject) {
        this.mScanDataObject = mObject;
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onDrawFrame(GL10 gl) {
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl.glClearDepthf(1.0f);
        gl.glClear(16640);
        gl.glMatrixMode(5888);
        gl.glLoadIdentity();
        gl.glShadeModel(7425);
        gl.glEnable(2929);
        gl.glDepthFunc(515);
        this.mScanDataObject.Render(gl);
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (this.mScanDataObject != null) {
            this.mScanDataObject.setScreenResolution(gl, width, height);
        }
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(5889);
        gl.glLoadIdentity();
        gl.glDisable(3024);
        gl.glActiveTexture(33984);
        gl.glHint(3152, 4354);
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        this.mScanDataObject.onSurfaceCreated(gl, config);
    }

    public boolean onCreateContextMenu(Menu menu) {
        return true;
    }

    public void setAngle(float angle) {
        this.mAngle = angle;
    }

    public float getAngle() {
        return this.mAngle;
    }
}
