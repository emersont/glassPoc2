
package com.ciandt.glass.poc;


import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.ciandt.glass.poc.eye.EyeEventReceiver;
import com.ciandt.glass.poc.eye.EyeEventReceiver.EyeEventListener;
import com.ciandt.glass.poc.head.HeadEventReceiver;
import com.ciandt.glass.poc.head.HeadEventReceiver.HeadEventListener;
import com.ciandt.glass.poc.tasks.MediaRecorderTask;
import com.ciandt.glass.poc.tasks.OpenSocketTask;
import com.ciandt.glass.poc.util.ConfigureLog4J;
import com.google.android.glass.eye.EyeGesture;
import com.google.android.glass.eye.EyeGestureManager;
import com.google.android.glass.media.Sounds;

/**
 * Main activity to control the PoC on Glass
 * @author emersont
 * Heavily based on samples from http://developer.android.com/guide/topics/media/camera.html
 */
public class MainActivity extends Activity
{
	
	final static String TAG = "GlassPoC";
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	
	private static final String SERVER_NAME = "172.16.57.65";
	private static final int SERVER_PORT = 9999;
	
	private static final Logger LOG = Logger.getLogger(MainActivity.class);
	
	private Socket mSocket;
	private ParcelFileDescriptor pfd;
	
    private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;

	// These are used for detecting blinks and winks
	private EyeGestureManager mEyeGestureManager;
	private EyeEventReceiver mEyeEventReceiver;
	private EyeEventListener mEyeEventListener;
	
	// Used for detecting head events
	private HeadEventReceiver mHeadEventReceiver;
	private HeadEventListener mHeadEventListener;

	private MediaRecorder mMediaRecorder;
	private boolean isRecording = false;
    
    private boolean previewing = true;
    private File currentFile;

    
    private EyeGestureReceiver mReceiver;

    private AudioManager mAudioManager;

   // private EyeGestureManager mEyeGestureManager;
    
    public SurfaceView mSurfaceView;
    public SurfaceHolder mSurfaceHolder;
    
    private MediaRecorderTask mRecTask;
    
    static {
        System.loadLibrary("iconv");
    } 

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ConfigureLog4J.configure();
        
        setContentView(R.layout.activity_main);
        
//        DummyView dummyView = new DummyView(this, this); //, previewCb, autoFocusCB);
//        FrameLayout dPreview = (FrameLayout)findViewById(R.id.mediarecorderFrame);
//        dPreview.addView(dummyView);

        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        new OpenSocketTask(this, SERVER_NAME, SERVER_PORT).execute();
        setupCamera();
        setupListeners();
        setupReceiver();
        
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

//        mEyeGestureManager = EyeGestureManager.from(this);
//
//        mReceiver = new EyeGestureReceiver();
   	
    }
    
    @Override
    protected void onStart() {
        super.onStart();

//        mEyeGestureManager.stopDetector(EyeGesture.DOUBLE_BLINK);
//        mEyeGestureManager.stopDetector(EyeGesture.WINK);
//        mEyeGestureManager.enableDetectorPersistently(EyeGesture.DOUBLE_BLINK, true);
//        mEyeGestureManager.enableDetectorPersistently(EyeGesture.WINK, true);
//
//        IntentFilter filter = new IntentFilter("com.google.glass.action.EYE_GESTURE");
//        filter.setPriority(5000);
//        registerReceiver(mReceiver, filter);
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");
        
//        mEyeGestureManager.stopDetector(EyeGesture.DOUBLE_BLINK);
//        mEyeGestureManager.stopDetector(EyeGesture.WINK);

//        unregisterReceiver(mReceiver);
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event   
        removeReceiver();
        finish();
    }
    
    @Override
    public void onResume() {
    	Log.d(TAG, "onResume called");
    	super.onResume();  // Always call the superclass method first
    	// Get the Camera instance as the activity achieves full user focus
    	if (mCamera == null) {
    	    setupCamera(); // Local method to handle camera init
    	}
    }

    @Override
    public void onPause() {
    	Log.d(TAG, "onPause called");
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event        
    }
    
    @Override
    public void onDestroy() {
    	Log.d(TAG, "onDestroy called");
    	super.onDestroy();    	
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event        
    	removeReceiver();
    	if (pfd != null) {
    		try {
				pfd.close();
			} catch (IOException e) {
				LOG.error("closing pfd: " + e.getMessage());
				e.printStackTrace();
			}
    	}
    	
    	if (mSocket != null) {
    		try {
				mSocket.close();
			} catch (IOException e) {
				LOG.error("closing mSocket: " + e.getMessage());
				e.printStackTrace();
			}    		
    	}
    }
    
    private void setupCamera() {
        autoFocusHandler = new Handler();
        
        //For some reason, right after launching from the "ok, glass" menu the camera is locked
        //Try 3 times to grab the camera, with a short delay in between.
        for(int i=0; i < 3; i++)
        {
	        mCamera = getCameraInstance();
	        if(mCamera != null) break;
	        
	        //Toast.makeText(this, "Couldn't lock camera, trying again in 1 second", Toast.LENGTH_SHORT).show();
	        try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
        if(mCamera == null)
        {
        	Toast.makeText(this, "Camera cannot be locked", Toast.LENGTH_SHORT).show();
        	finish();
        }

        mPreview = new CameraPreview(this, mCamera); //, previewCb, autoFocusCB);
       // mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
        FrameLayout preview = (FrameLayout)findViewById(R.id.cameraPreview);
        preview.addView(mPreview);
    }

    /** A safe way to get an instance of the Camera object. */
    public Camera getCameraInstance(){
       if (mCamera == null) {
        try {
            mCamera = Camera.open();
            Log.d(TAG, "getCamera = " + mCamera);
        } catch (Exception e){
        	Log.d(TAG, e.toString());
        }
       }
        return mCamera;
    }
    
    private void setupListeners() {
    	LOG.debug("setupListeners called");
    	Log.d(TAG, "setupListeners called");
    	
		Log.d(TAG, "--------------------------------- setting up head event listener");
		// Setup for the head detector
		mHeadEventListener = new HeadEventListener(){
			public void onHeadEvent(float zMagnitude){
				Log.d(TAG, "--------------------------------- head event z:" + zMagnitude);
			}
		};

		Log.d(TAG, "--------------------------------- creating new head event receiver");
		mHeadEventReceiver = new HeadEventReceiver(getApplicationContext(), mHeadEventListener);
    	
		// Setup for the eye gestures
		mEyeGestureManager = EyeGestureManager.from(getApplicationContext());

		mEyeEventListener = new EyeEventListener() {
			@Override
			public void onWink() {
		    	LOG.debug("--------------------------------- wink detected ----------------------------");
		    	Log.d(TAG, "--------------------------------- wink detected ----------------------------");
				Toast.makeText(getApplicationContext(), "Wink. Exiting...", Toast.LENGTH_SHORT).show();

		        Intent mHomeIntent =  new Intent(Intent.ACTION_MAIN, null); 
		        mHomeIntent.addCategory(Intent.CATEGORY_HOME); 
		        mHomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
		                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED); 
		        startActivity(mHomeIntent);

			}

			@Override
			public void onDoubleBlink() {
				mAudioManager.playSoundEffect(Sounds.SUCCESS);
//				if (isRecording) {
//					Toast.makeText(getApplicationContext(), "Stop Rec", Toast.LENGTH_SHORT).show();
//					isRecording = false;
//				} else {
//					Toast.makeText(getApplicationContext(), "Recording", Toast.LENGTH_SHORT).show();
//					isRecording = true;
//					
//				}
				
		    	//LOG.debug("double wink detected");
		    	Log.d(TAG, "---------------------------------double wink detected  is rec: " + isRecording);
//				Toast.makeText(getApplicationContext(), "WinkWink", Toast.LENGTH_SHORT).show();
				
				LOG.info("double wink detected isRecording: " + isRecording);
                if (isRecording) {
                	//mRecTask.cancel(true);
                    // stop recording and release camera
                	Toast.makeText(getApplicationContext(), "Stopping Rec", Toast.LENGTH_SHORT).show();
                	
                	try{
                	mMediaRecorder.stop();  // stop the recording
                	} catch (IllegalStateException e) {
                		Log.e(TAG, "IllegalStateException stopping media rec:" + e.getMessage());
                		LOG.error("IllegalStateException stopping media rec: " + e.getMessage());
                	}
                    releaseMediaRecorder(); // release the MediaRecorder object
            		Log.d(TAG, "locking camera");
            		LOG.debug("Locking Camerar");
                    mCamera.lock();         // take camera access back from MediaRecorder

                    // inform the user that recording has stopped
                    Toast.makeText(getApplicationContext(), "Rec Stopped", Toast.LENGTH_SHORT).show();
                    isRecording = false;
                } else {
                    // initialize video camera
                	 Toast.makeText(getApplicationContext(), "Rec Starting", Toast.LENGTH_SHORT).show();
                	 startRecordingComplete();
                    //startRecording();                	 
                	//startLongRunningOperation();
                    Toast.makeText(getApplicationContext(), "Recording", Toast.LENGTH_SHORT).show();
                        // Camera is available and unlocked, MediaRecorder is prepared,
                        // now you can start recording
                    	//startRecording();
                        isRecording = true;
//                    } else {
//                    	isRecording = false;
//                        // prepare didn't work, release the camera
//                        releaseMediaRecorder();
//                        // inform user
//                    }
                }
				
			}
		};

		mEyeEventReceiver = new EyeEventReceiver(mEyeEventListener);
		
    }
  
    
	public void setupReceiver() {
    	LOG.debug("setupReceiver called");
    	Log.d(TAG, "setupReceiver called");
    	
		// Eye Events
		mEyeGestureManager.stopDetector(EyeGesture.DOUBLE_BLINK);
		mEyeGestureManager.stopDetector(EyeGesture.WINK);

		mEyeGestureManager.enableDetectorPersistently(EyeGesture.DOUBLE_BLINK,
				true);
		mEyeGestureManager.enableDetectorPersistently(EyeGesture.WINK, true);

		IntentFilter eyeFilter = new IntentFilter(
				"com.google.glass.action.EYE_GESTURE");
		eyeFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY-1);

		getApplicationContext().registerReceiver(mEyeEventReceiver, eyeFilter);
		
		// Head Events
		//mHeadEventReceiver.startListening();
	}

	public void removeReceiver() {
		// Eye Events
		mEyeGestureManager.stopDetector(EyeGesture.DOUBLE_BLINK);
		mEyeGestureManager.stopDetector(EyeGesture.WINK);

		try{
			getApplicationContext().unregisterReceiver(mEyeEventReceiver);
		} catch (Exception e){
			e.printStackTrace();
		}
		
		// Head Events
		mHeadEventReceiver.stopListening();
	}

    /**
     * release camera
     */
    private void releaseCamera() {
    	Log.d(TAG, "releaseCamera called mCamera = " + mCamera);
    	
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();   // release the camera for other applications
            mCamera = null;
        }
    }
    
    private void releaseMediaRecorder(){
    	Log.d(TAG, "releaseMediaRecorder called mMediaRecorder = " + mMediaRecorder);

        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }
    
    protected void startLongRunningOperation() {
    	Log.d(TAG, "startLongRunningOperation");
//        Log.d(TAG, "hidding preview display");
//        mPreview.setVisibility(android.view.View.INVISIBLE);
        // Fire off a thread to do some work that we shouldn't do directly in the UI thread
        Thread t = new Thread() {
            public void run() {
            	startRecordingComplete();
            }
        };
        t.start();
    }

    
    public void startRecording() {
    	mRecTask = (MediaRecorderTask) new MediaRecorderTask(this, SERVER_NAME, SERVER_PORT, mCamera, mSurfaceHolder).execute();
    }
    
    public boolean startRecordingComplete() {
    	Log.d(TAG, "startRecording media recorder is supposed to be prepared");
    	LOG.info("will start media recorder");
    	 mMediaRecorder = new MediaRecorder();
    	 
    	 /* Acordingly to 
    	  * #6 josephba...@gmail.com
		  * I had this problem when triggering a video capturing in my app while the video preview was live.  After much wailing and gnashing of teeth, I figured out that to start video capture on a previewing camera, I had to first completely stop the video preview.  This was accomplished with the following code, which I put of the top of my prepareVideoRecorder() implementation, prior to doing anything with a MediaRecorder:
    	  */
    	 try {
    		 Log.d(TAG, "setting camera preview to nulll");
    			mCamera.setPreviewDisplay(null);
    		} catch (java.io.IOException ioe) {
    			Log.d(TAG, "IOException nullifying preview display: " + ioe.getMessage());
    		}
    		mCamera.stopPreview();
    		mCamera.unlock();
    		
    	    mMediaRecorder.setCamera(mCamera);
    	    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
    	    mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
    	    /* Use hidden interface to write in to stream-able
    	    container(mpeg2ts). Currently it support only H264/AAC, but this enough
    	    for most situations defined in frameworks/base/include/media/mediarecorder.h
    	    and in frameworks/base/media/java/android/media/MediaRecorder.java
    	    *** @hide H.264/AAC data encapsulated in MPEG2/TS
    	    public static final int OUTPUT_FORMAT_MPEG2TS = 8;
    	    *** AAC audio codec
    	    public static final int AAC = 3
    	    */    	    
    	    /*
  recorder.setOutputFormat(8);
  recorder.setAudioEncoder(3);
  recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
  recorder.setVideoFrameRate(20);
  recorder.setVideoSize(640, 480);
  recorder.setVideoEncodingBitRate(1024*1024);
    	     */
    	    
    	    // tried setVideoFrameRate here, but doesn' work gives 
    	    //  setVideoFrameRate called in an invalid state: 2
       	 Log.d(TAG, "222222222222222222222222222222222222222");
    	    //already tested mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
       	 	// this works        	 	mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);       	 	
       	 	// does not work mMediaRecorder.setOutputFormat(8); // 8 is OUTPUT_FORMAT_MPEG2TS
       	mMediaRecorder.setOutputFormat(7); // OUTPUT_FORMAT_RTP_AVP
       	 	// does not work mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
       	 	//already tested mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
       	 	// this works mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
       	mMediaRecorder.setAudioEncoder(3);
       	 	//already tested mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
       	 	// this works mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
       		// seems to work 
       	mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
       		// does not work mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
       		// does not work mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
       //	
       	mMediaRecorder.setVideoEncodingBitRate(1024*1024);
    	    mMediaRecorder.setVideoFrameRate(15);
    	    
            // Then make sure you set both preview and video to the same size (if they were different in my experience preview would freeze when video record started):
    	    mMediaRecorder.setVideoSize(640, 480);    	    
            mMediaRecorder.setAudioChannels(2); // stereo
          	 Log.d(TAG, "3333333333333333333333333333333333333333");
          	 
          	 /*
          	  * #6 josephba...@gmail.com
I had this problem when triggering a video capturing in my app while the video preview was live.  After much wailing and gnashing of teeth, I figured out that to start video capture on a previewing camera, I had to first completely stop the video preview.  This was accomplished with the following code, which I put of the top of my prepareVideoRecorder() implementation, prior to doing anything with a MediaRecorder:

	try {
		mCamera.setPreviewDisplay(null);
	} catch (java.io.IOException ioe) {
		Log.d(TAG, "IOException nullifying preview display: " + ioe.getMessage());
	}
	mCamera.stopPreview();
	mCamera.unlock();

Subsequently, I am able to capture video.  I am on XE12.
          	  */
          	 
 	        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());
 	       // mMediaRecorder.setPreviewDisplay(getmSurfaceHolder().getSurface());
 	        //mMediaRecorder.setPreviewDisplay(null);
//    	    mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());
    	    //mMediaRecorder.setMaxDuration((int) -1);
        	//LOG.info("prepareVideoRecorder setting output to socket");
    	    // code to output video to file
//            currentFile = getOutputMediaFile(MEDIA_TYPE_VIDEO);
//            mMediaRecorder.setOutputFile(currentFile.toString());
    	    // mMediaRecorder.setOutputFile("/sdcard/sample.mov");
      
            
            // verify if socket is opened, if not wait at most 5 seconds
            // after that give up
            int maxTries = 0;
		  while (mSocket == null && maxTries < 10) {
			try {
				Thread.sleep(500);
				maxTries++;
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		  }
		  if (mSocket == null) {
			  return false;
		  }
		// Step 4: Set output file to the socket
            Log.d(TAG, "Setting output file");
        	if (pfd == null) {    		
        		pfd = ParcelFileDescriptor.fromSocket(mSocket);
        		LOG.debug("ParcelFileDescriptor was null, now is: " + pfd);
        	}
        	mMediaRecorder.setOutputFile(pfd.getFileDescriptor());
            
    	    try {
    	    	Log.d(TAG, "444444444444444444444444444444444444444444");
    	    	mMediaRecorder.prepare();
    	    	Log.d(TAG, "555555555555555555555555555555555555555555");
    	    	Toast.makeText(getApplicationContext(), "Recording", Toast.LENGTH_SHORT).show();

    	    	mMediaRecorder.start();
    	    	Log.d(TAG, "rerererererererererecrecercerecercercere recrec");
//    	       	 try {
//    	       		Log.d(TAG, "setting camera preview again");
//    	  			mCamera.setPreviewDisplay(mPreview.getHolder());
//    	  		} catch (java.io.IOException ioe) {
//    	  			Log.d(TAG, "IOException nullifying preview display: " + ioe.getMessage());
//    	  		}
    	        return true;

    	    } catch (Exception e) {
    	        return false;
    	    }
    	    

    }
    
    private boolean prepareVideoRecorder(){

    	/*MediaRecorder recorder = new MediaRecorder();
 recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
 recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
 recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
 recorder.setOutputFile(PATH_NAME);
 recorder.prepare();
 recorder.start();   // Recording is now started
 */
    	
    	LOG.info("prepareVideoRecorder Camera:" + mCamera);
        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        LOG.info("prepareVideoRecorder unlocking camera");
        Log.d(TAG, "Media recorder was instantiated, unlocking camera");
        mCamera.unlock();
        Log.d(TAG, "Camera was unlocked, setting camera on media recorder");
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        Log.d(TAG, "Setting video source on media recorder");
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        Log.d(TAG, "Setting audio source on media recorder");
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        
        // Video Settings 
        // Glass camera does not support setVideoSize
        //Log.d(TAG, "Setting video size");
        //mMediaRecorder.setVideoSize(640, 480);
        
        // mininum setting for frame rate
        //Log.d(TAG, "Setting video frame rate");
        //mMediaRecorder.setVideoFrameRate(15);
        
        Log.d(TAG, "Setting output format");
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        //mMediaRecorder.setVideoEncodingBitRate(VIDEO_BITRATE);
        // Audio Settings
        Log.d(TAG, "Setting audio encoder");
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT); 
        Log.d(TAG, "Setting audio channels");
        mMediaRecorder.setAudioChannels(2); // mono
        Log.d(TAG, "Setting audio sampling rate");
        mMediaRecorder.setAudioSamplingRate(8);
        //mMediaRecorder.setAudioEncodingBitRate(AUDIO_BITRATE);
        
        // Following code does the same as getting a CamcorderProfile (but customizable)


        Log.d(TAG, "Setting video encoder to default");
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT); 
//        openSocket();
//        while (mSocket == null) {
//	    	try {
//				Thread.sleep(500);
//			} catch (InterruptedException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//        }	
//    	LOG.info("prepareVideoRecorder socket " + mSocket);
//    	if (mSocket != null) {
//    		LOG.info("prepareVideoRecorder socket isbound "+ mSocket.isBound());
//    	}
//    	
    	//ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(mSocket);
        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        //Log.d(TAG, "Setting profile to camcorder");
        //mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));


    	//LOG.info("prepareVideoRecorder setting output to socket");
        currentFile = getOutputMediaFile(MEDIA_TYPE_VIDEO);
  
        // Step 4: Set output file to the socket
        Log.d(TAG, "Setting output file");
        mMediaRecorder.setOutputFile(currentFile.toString());
        //mMediaRecorder.setOutputFile(pfd.getFileDescriptor());
        

        // Step 5: Set the preview output
        Log.d(TAG, "Setting preview display");
        mPreview.setVisibility(android.view.View.INVISIBLE);
        //mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());
        mMediaRecorder.setPreviewDisplay(getmSurfaceHolder().getSurface());
        //mMediaRecorder.setPreviewDisplay(null);

        LOG.info("prepareVideoRecorder will prepare mediarecorder");
        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
            LOG.info("Media recorder was prepared!!!");
            Log.d(TAG, "Media recorder was prepared!!!");
        } catch (IllegalStateException e) {
        	LOG.error("IllegalStateException preparing MediaRecorder: " + e.getMessage());

            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
        	LOG.error("IOException preparing MediaRecorder: " + e.getMessage());
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
    	Log.d(TAG, "startRecording media recorder is supposed to be prepared");
    	LOG.info("will start media recorder");
    	try {
//    		Log.d(TAG, "workaround... stopping mediarecorder : " + mMediaRecorder);
//    		mMediaRecorder.stop();
//    		mCamera.unlock();
    		Log.d(TAG, "now starting record");
    		mMediaRecorder.start();
    	} catch (IllegalStateException e) {
    		Log.e(TAG, "IllegalStateException start" + e.getMessage());
    		LOG.error("IllegalStateException start: " + e.getMessage());
    	} 

        // inform the user that recording has started
        Toast.makeText(getApplicationContext(), "Recording", Toast.LENGTH_SHORT).show();
        return true;
    }
   

    
    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
          return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                  Environment.DIRECTORY_PICTURES), "glasspoc");
        
    	LOG.error("mediaStorageDir: " + mediaStorageDir);
        Log.d(TAG, "mediaStorageDir: " + mediaStorageDir);
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.
        // This method returns the standard, shared and recommended location 
        // for saving pictures and videos. 

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
            "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
            "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }
    	LOG.error("mediaFile: " + mediaFile);
        Log.d(TAG, "mediaFile: " + mediaFile);
        return mediaFile;
    }
    
    // Mimic continuous auto-focusing
    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
            public void onAutoFocus(boolean success, Camera camera) {
                autoFocusHandler.postDelayed(doAutoFocus, 1000);
            }
    };
    
    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (previewing)
                mCamera.autoFocus(autoFocusCB);
        }
    };
    
    PreviewCallback previewCb = new PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
        	// do nothing
        }
    };

	/**
	 * @return the mSocket
	 */
	public Socket getmSocket() {
		return mSocket;
	}

	/**
	 * @param mSocket the mSocket to set
	 */
	public void setmSocket(Socket mSocket) {
		this.mSocket = mSocket;
	}
	
    class EyeGestureReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            mAudioManager.playSoundEffect(Sounds.SUCCESS);
            Bundle extras = intent.getExtras();

            String eyeGesture = extras.getString("gesture");
            boolean screenOff = extras.getBoolean("screen_off");

            Log.d(TAG, eyeGesture + " is detected");

            Toast.makeText(getApplicationContext(), "Detected " + eyeGesture + "!", Toast.LENGTH_SHORT).show();
            if (EyeGesture.DOUBLE_BLINK.toString().equals(eyeGesture)) {
            	if (!isRecording) {
            		Toast.makeText(getApplicationContext(), "Recording", Toast.LENGTH_SHORT).show();
            		 Log.d(TAG, eyeGesture + "Starting MediaRecord record");
            		//startRecording();
            		
            		//String server, int port, Camera cam, SurfaceHolder surfaceHolder) {
            		runOnUiThread(new Runnable() {
            		    public void run() {
            		    	startRecordingComplete();
            		    }
            		});
            		isRecording = true;
             	} else {
                	Toast.makeText(getApplicationContext(), "Stopping Rec", Toast.LENGTH_SHORT).show();
                	mRecTask.cancel(true);
                	isRecording = false;
/*                	
                	try{
               		 Log.d(TAG, eyeGesture + "Stopping MediaRecord");
               		 	mMediaRecorder.stop();  // stop the recording
                		Log.d(TAG, eyeGesture + "MediaRecord Stopped");
                	} catch (IllegalStateException e) {
                		Log.e(TAG, "IllegalStateException stopping media rec:" + e.getMessage());
                		LOG.error("IllegalStateException stopping media rec: " + e.getMessage());
                	}
                    releaseMediaRecorder(); // release the MediaRecorder object
            		Log.d(TAG, "locking camera");
            		LOG.debug("Locking Camerar");
                    mCamera.lock();         // take camera access back from MediaRecorder

                    // inform the user that recording has stopped
                    Toast.makeText(getApplicationContext(), "Rec Stopped", Toast.LENGTH_SHORT).show();
                    isRecording = false;
                    
                    if (mSocket != null) {
                		try {Log.d(TAG, eyeGesture + "Closing socket");
            				mSocket.close();
            			} catch (IOException e) {
            				LOG.error("closing mSocket: " + e.getMessage());
            				e.printStackTrace();
            			}    		
                	}
*/                	
             	}
            }

            abortBroadcast();
        }
    }

	/**
	 * @return the mSurfaceHolder
	 */
	public SurfaceHolder getmSurfaceHolder() {
		return mSurfaceHolder;
	}

	/**
	 * @param mSurfaceHolder the mSurfaceHolder to set
	 */
	public void setmSurfaceHolder(SurfaceHolder mSurfaceHolder) {
		Log.d(TAG, "setting surface holder: " +  mSurfaceHolder);
		this.mSurfaceHolder = mSurfaceHolder;
	}
}
