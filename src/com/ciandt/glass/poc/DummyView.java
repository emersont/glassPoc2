package com.ciandt.glass.poc;

import android.content.Context;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class DummyView  extends SurfaceView implements SurfaceHolder.Callback {

private SurfaceHolder surfaceHolder;
private MainActivity mainActivity;


public DummyView(Context context, MainActivity ma) {
    super(context);
    
    mainActivity = ma;

    // Install a SurfaceHolder.Callback so we get notified when the
    // underlying surface is created and destroyed.
    surfaceHolder = getHolder();
    surfaceHolder.addCallback(this);
}

@Override
public void surfaceCreated(SurfaceHolder holder) {
    // Perform actions that require Surface to have been created here e.g.
    // mMediaRecorder.setPreviewDisplay(holder.getSurface()); etc.
	mainActivity.setmSurfaceHolder(holder);

}

@Override
public void surfaceDestroyed(SurfaceHolder holder) {
    // Take care of releasing camera preview etc.
}

@Override
public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    // Handle changes
}

}