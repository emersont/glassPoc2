package com.ciandt.glass.poc.tasks;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

import com.ciandt.glass.poc.MainActivity;

import de.mindpipe.android.logging.log4j.LogConfigurator;

public class MediaRecorderTask extends AsyncTask<Void, Void, Void> {
	
	final static String TAG = "MediaRecorderTask";
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	
	private MainActivity parent;
	private Socket mSocket;
	private ParcelFileDescriptor pfd;
	private String server;
	private int port;
    private Camera mCamera;
	private MediaRecorder mMediaRecorder;
	public SurfaceHolder mSurfaceHolder;
	private boolean initialized = false;

	private static final Logger LOG = Logger.getLogger(MediaRecorderTask.class);
	
	public MediaRecorderTask(MainActivity main, String server, int port, Camera cam, SurfaceHolder surfaceHolder) {
		LOG.info("MediaRecorderTask server " + server + " port " + port);
		Log.d(TAG, " server " + server + " port " + port);
		
		this.parent = main;	
		this.server = server;
		this.port = port;
		this.mCamera = cam;
		this.mSurfaceHolder = surfaceHolder;
		Log.d(TAG, " MediaRecorderTask started at " + System.currentTimeMillis());
	}
	
	@Override
    protected Void doInBackground(Void... voids) {
		Log.d(TAG, " execute  called... doInBackground!!!!!!!!!!!!!!!!!!!!!11111111 at " + System.currentTimeMillis());
        LOG.info("doInBackground ...............................");
// 	   try {
// 		   	Log.d(TAG, " doInBackground trying to open socket");
// 		  	mSocket = new Socket(server, port);
// 		  	LOG.info("OpenSocketTask socket " + mSocket);
// 		  	Log.d(TAG, "opened socket " + mSocket);
//		} catch (UnknownHostException e) {
//			 Log.d(TAG, " trying to create a new socket");
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} 		  	
	  	LOG.info("will start media recorder");
	  	Log.d(TAG,"will start media recorder");
    	 mMediaRecorder = new MediaRecorder();
    	 
    	 /* Acordingly to 
    	  * #6 josephba...@gmail.com
		  * I had this problem when triggering a video capturing in my app while the video preview was live.  After much wailing and gnashing of teeth, I figured out that to start video capture on a previewing camera, I had to first completely stop the video preview.  This was accomplished with the following code, which I put of the top of my prepareVideoRecorder() implementation, prior to doing anything with a MediaRecorder:
    	  */
    	 	try {
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
       	 	// this works 
       	 	mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
       	 	// mMediaRecorder.setOutputFormat(8); // 8 is OUTPUT_FORMAT_MPEG2TS
       	 	//already tested mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
       	 	// this works 
       	 	mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
       		//mMediaRecorder.setAudioEncoder(3);
       	 	//already tested mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
       	 	// this works 
       	 	mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
       		//mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
       	 	
    	    mMediaRecorder.setVideoFrameRate(15);
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
    	    mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
    	    mMediaRecorder.setMaxDuration((int) -1);
        	//LOG.info("prepareVideoRecorder setting output to socket");
    	    // code to output video to file
 	            File currentFile = getOutputMediaFile(MEDIA_TYPE_VIDEO);
 	            mMediaRecorder.setOutputFile(currentFile.toString());
    	    // mMediaRecorder.setOutputFile("/sdcard/sample.mov");
 	      
            // Step 4: Set output file to the socket
//            Log.d(TAG, "Setting output file");
//        	if (pfd == null) {    		
//        		pfd = ParcelFileDescriptor.fromSocket(mSocket);
//        		LOG.debug("ParcelFileDescriptor was null, now is: " + pfd);
//        	}
//        	mMediaRecorder.setOutputFile(pfd.getFileDescriptor());
//            
    	    try {
    	    	Log.d(TAG, "444444444444444444444444444444444444444444");
    	    	mMediaRecorder.prepare();
    	    	Log.d(TAG, "555555555555555555555555555555555555555555");
    	    	Toast.makeText(parent, "Recording", Toast.LENGTH_SHORT).show();
    	    	mMediaRecorder.start();
    	    	Log.d(TAG, "rerererererererererecrecercerecercercere ererecc");

    	    } catch (Exception e) {
    	    	Log.e(TAG, "Error " + e.getMessage());
    	    } 
    	    initialized = true;

        return null;
    }
	
	
    /**
     * 
     */
	@Override
    protected void onPostExecute(Void time) {
    	Log.e("MediaRecorderTask", "returning at " + System.currentTimeMillis());
    	releaseMediaRecorder();

    }
	
   @Override
    protected void onCancelled() {
	   	Log.e(TAG, "onCancelled  fired at " + System.currentTimeMillis());
	   	if (initialized) {
	   		releaseMediaRecorder();
	   	}
    }
	
    private void releaseMediaRecorder(){
    	Log.d(TAG, "releaseMediaRecorder called mMediaRecorder = " + mMediaRecorder +  " at "+ System.currentTimeMillis());

        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
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

}