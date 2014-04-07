package com.ciandt.glass.poc.head;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class HeadEventReceiver implements SensorEventListener{
	
	private static final String TAG = "HeadEventReceiver";

	public static interface HeadEventListener {
		public void onHeadEvent(float zMagnitude);
	}

	private Context mContext;
	
	private SensorManager mSensorManager;
	private Sensor mGravity;

	private HeadEventListener mListener;

	public HeadEventReceiver(Context context, HeadEventListener listener){
		this(context);
		mListener = listener;
	}
	
	public HeadEventReceiver(Context context){
		mContext = context;

		mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
		mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
	}

	public void setHeadEventListener(HeadEventListener listener){
		mListener = listener;
	}

	public void startListening(){
		mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_NORMAL);
	}

	public void stopListening(){
		mSensorManager.unregisterListener(this);
	}


	@Override
	public void onSensorChanged(SensorEvent event){
//		Log.d(TAG, "X axis: " + event.values[0]);
//		Log.d(TAG, "Y axis: " + event.values[1]);
//		Log.d(TAG, "Z axis: " + event.values[2]);

		if(mListener != null){
			mListener.onHeadEvent(event.values[2]);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy){
		// Do nothing
	}

}
