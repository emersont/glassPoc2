package com.ciandt.glass.poc;

import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspServer;
import net.majorkernelpanic.streaming.video.VideoQuality;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.ciandt.glass.poc.head.HeadEventReceiver;
import com.ciandt.glass.poc.head.HeadEventReceiver.HeadEventListener;
import com.ciandt.glass.poc.util.Utils;

/**
 * A simple activity to start/stop a RTSP server using head gestures
 */
public class RTSPMainActivity extends Activity {

	private final static String TAG = "RTSPMainActivity";
	
	// define z-axis value correspondent to a +/- 45 degrees up
	private static final float HEAD_UP_Z_THRESHOLD = -4.5f;

	// interval in milliseconds between head gestures.
	// head events will not be considered if occurs in intervals 
	// lower than this threshold 
	private static final int HEAD_EVENT_INTERVAL_THRESHOLD = 2000;
	
	// used to preview the video
	private SurfaceView mSurfaceView;
	
	// These are used for detecting blinks and winks
//	private EyeGestureManager mEyeGestureManager;
//	private EyeEventReceiver mEyeEventReceiver;
//	private EyeEventListener mEyeEventListener;
	
	// Used for detecting head events
	private HeadEventReceiver mHeadEventReceiver;
	private HeadEventListener mHeadEventListener;

	// Gesture detector on the touchpad on the side of Glass
	//private GestureDetector mGestureDetector;
	
	// just to play funny sounds
	private AudioManager mAudioManager;
	
	private boolean isTransmitting = true;
	
	// time of the last head event
	private long mLastHeadEvent = 0;
	
	// just to make sure network is available
	ConnectivityManager mConnectivity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		mConnectivity =  (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

		// Skip if no connection, or background data disabled
		NetworkInfo info = mConnectivity.getActiveNetworkInfo();
		if (info == null || !info.isAvailable()) {
			Log.e(TAG,"No network connection available. Exiting application");
			Toast.makeText(getApplicationContext(), "No network available. Exiting...", Toast.LENGTH_LONG).show();
			return;
		}
		Log.d(TAG, "oncreate ----------------------------");
		
		//mGestureDetector = createGestureDetector(this);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		Log.d(TAG, "creating surface ----------------------------");
		mSurfaceView = (SurfaceView) findViewById(R.id.surface);
		
		Log.d(TAG, "creating editor ----------------------------");
		
		// Sets the port of the RTSP server to 1234
		Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		editor.putString(RtspServer.KEY_PORT, String.valueOf(1234));
		editor.commit();
		Log.d(TAG, "creating session ----------------------------");

		// Configures the SessionBuilder
		SessionBuilder.getInstance()
		.setSurfaceView(mSurfaceView)
//		.setPreviewOrientation(90)
		.setVideoQuality(new VideoQuality(640,480,15,4500000))
		.setContext(getApplicationContext())
		.setAudioEncoder(SessionBuilder.AUDIO_AAC)
		.setVideoEncoder(SessionBuilder.VIDEO_H264);
		
		
		
		Log.d(TAG, "starting rtsp server  ----------------------------");
		// Starts the RTSP server
		Toast.makeText(getApplicationContext(), "Starting Transmission", Toast.LENGTH_SHORT).show();
		
		//this.stop
		Log.d(TAG, "rtsp server intent created ----------------------------");
		
		// Starts the RTSP server
		//this.startService(new Intent(this,RtspServer.class));
		this.startService(new Intent(this,RtspServer.class));
		Log.d(TAG, "rtsp server intent started ----------------------------");

		Toast.makeText(getApplicationContext(), "Wait...", Toast.LENGTH_LONG).show();

		//Toast.makeText(this, "Transmitting...", Toast.LENGTH_SHORT).show();
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				
        setupListeners();
        setupReceiver();

		String ipAddress = Utils.getIPAddress(true); // IPv4
		Toast.makeText(getApplicationContext(), "Transmitting at rtsp://" + ipAddress + ":1234", Toast.LENGTH_LONG).show();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		removeReceiver();
	}
	
	/**
	 * Setup the gesture listeners
	 */
	private void setupListeners() {
    	Log.d(TAG, "setupListeners called");
    	
		Log.d(TAG, "--------------------------------- setting up head event listener");
		// Setup for the head detector
		mHeadEventListener = new HeadEventListener(){
				public void onHeadEvent(float zMagnitude){
//					Log.d(TAG, "--------------------------------- head event z:" + zMagnitude);
			
					
					long currTime = System.currentTimeMillis();
					if (currTime - mLastHeadEvent > HEAD_EVENT_INTERVAL_THRESHOLD) {
						if (zMagnitude < HEAD_UP_Z_THRESHOLD) {
							Log.d(TAG, "--------------------------------- head event z:" + zMagnitude + " is transmitting: " + isTransmitting);
							if (isTransmitting) {
			                	Toast.makeText(getApplicationContext(), "Stop Transmission", Toast.LENGTH_SHORT).show();
			                	RTSPMainActivity.this.stopService(new Intent(RTSPMainActivity.this,RtspServer.class));
			                	Toast.makeText(getApplicationContext(), "Stopped", Toast.LENGTH_LONG).show();
								
						        Intent mHomeIntent =  new Intent(Intent.ACTION_MAIN, null); 
						        mHomeIntent.addCategory(Intent.CATEGORY_HOME); 
						        mHomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
						                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED); 
						        startActivity(mHomeIntent);
			                    isTransmitting = false;
//							} else {
//			                	Toast.makeText(getApplicationContext(), "Startint Transmission", Toast.LENGTH_SHORT).show();
//			                	getApplicationContext().startService(new Intent(getApplicationContext(),RtspServer.class));
//			                    Toast.makeText(getApplicationContext(), "Transmitting...", Toast.LENGTH_LONG).show();
//		                        isTransmitting = true;                                   //    com.ciandt.glass.poc.RTSPMainActivity
//						        Intent rtspMainActivity =  new Intent(getApplicationContext(), com.ciandt.glass.poc.RTSPMainActivity.class); 
//						        startActivity(rtspMainActivity);
							}
						}
						mLastHeadEvent = currTime;
					}
				}
			};

			Log.d(TAG, "--------------------------------- creating new head event receiver");
			mHeadEventReceiver = new HeadEventReceiver(getApplicationContext(), mHeadEventListener);
	    	
			// Setup for the eye gestures
//			mEyeGestureManager = EyeGestureManager.from(getApplicationContext());

//			mEyeEventListener = new EyeEventListener() {
//				@Override
//				public void onWink() {
//			    	Log.d(TAG, "--------------------------------- wink detected ----------------------------");
//			    	mAudioManager.playSoundEffect(Sounds.DISALLOWED);
//					Toast.makeText(getApplicationContext(), "Exiting...", Toast.LENGTH_SHORT).show();
//
//			        Intent mHomeIntent =  new Intent(Intent.ACTION_MAIN, null); 
//			        mHomeIntent.addCategory(Intent.CATEGORY_HOME); 
//			        mHomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
//			                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED); 
//			        startActivity(mHomeIntent);
//				}
//				
//				@Override
//				public void onDoubleBlink() {
//					mAudioManager.playSoundEffect(Sounds.SUCCESS);
//					Log.d(TAG, "---------------------------------double wink detected  is rec: " + isTransmitting);					
//				}
////
//				@Override
//				public void onDoubleBlink() {
//					mAudioManager.playSoundEffect(Sounds.SUCCESS);
//
//			    	Log.d(TAG, "---------------------------------double wink detected  is rec: " + isTransmitting);
//					
//			    	Log.d(TAG, "double wink detected isTransmitting: " + isTransmitting);
//	                if (isTransmitting) {
//
//	                	Toast.makeText(getApplicationContext(), "Stop Transmission", Toast.LENGTH_SHORT).show();
//	                	getApplicationContext().stopService(rtspServer);
//	                	Toast.makeText(getApplicationContext(), "Stopped", Toast.LENGTH_SHORT).show();
//	                    isTransmitting = false;
//	                    return;
//	                } else {
//	                	Toast.makeText(getApplicationContext(), "Start Transmission", Toast.LENGTH_SHORT).show();
//		                getApplicationContext().startService(rtspServer);
//	                    Toast.makeText(getApplicationContext(), "Transmitting...", Toast.LENGTH_SHORT).show();
//                        isTransmitting = true;
//                        return;
//	                }
//				}
//			};
//
//			mEyeEventReceiver = new EyeEventReceiver(mEyeEventListener);
			
	}
	  
	    
		/**
		 * Initialize the gesture listeners
		 */
		public void setupReceiver() {
	    	Log.d(TAG, "setupReceiver called");
	    	
			// Eye Events
//			mEyeGestureManager.stopDetector(EyeGesture.DOUBLE_BLINK);
//			mEyeGestureManager.stopDetector(EyeGesture.WINK);
//
//			mEyeGestureManager.enableDetectorPersistently(EyeGesture.DOUBLE_BLINK,
//					true);
//			mEyeGestureManager.enableDetectorPersistently(EyeGesture.WINK, true);
//
//			IntentFilter eyeFilter = new IntentFilter(
//					"com.google.glass.action.EYE_GESTURE");
//			eyeFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY-1);
//
//			getApplicationContext().registerReceiver(mEyeEventReceiver, eyeFilter);
			
			// Head Events
			mHeadEventReceiver.startListening();
		}

		
		/**
		 * Stop the gesture events receivers
		 */
		public void removeReceiver() {
			// Eye Events
//			mEyeGestureManager.stopDetector(EyeGesture.DOUBLE_BLINK);
//			mEyeGestureManager.stopDetector(EyeGesture.WINK);
//
//			try{
//				getApplicationContext().unregisterReceiver(mEyeEventReceiver);
//			} catch (Exception e){
//				e.printStackTrace();
//			}
			
			// Head Events
			mHeadEventReceiver.stopListening();
		}
		
//		private GestureDetector createGestureDetector(Context context) {
//		    GestureDetector gestureDetector = new GestureDetector(context);
//		        //Create a base listener for generic gestures
//		        gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
//		            @Override
//		            public boolean onGesture(com.google.android.glass.touchpad.Gesture gesture) {
//		                if (gesture == Gesture.SWIPE_DOWN) {
//		                	removeReceiver();		                	
//		                }
//		                return false;
//		            }
//
//		        });
//
//		        return gestureDetector;
//		    }
}
