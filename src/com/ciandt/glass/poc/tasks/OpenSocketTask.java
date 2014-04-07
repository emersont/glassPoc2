package com.ciandt.glass.poc.tasks;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.ciandt.glass.poc.MainActivity;

import de.mindpipe.android.logging.log4j.LogConfigurator;

public class OpenSocketTask extends AsyncTask<Void, Void, Void> {
	
	private MainActivity parent;
	private Socket mSocket;
	private String server;
	private int port;

	private static final Logger LOG = Logger.getLogger(OpenSocketTask.class);
	
	public OpenSocketTask(MainActivity main, String server, int port) {
		LOG.info("OpenSocketTask server " + server + " port " + port);
		Log.e("OpenSocketTask", " server " + server + " port " + port);
		parent = main;	
		this.server = server;
		this.port = port;
	}
	
	@Override
    protected Void doInBackground(Void... voids) {
        LOG.info("doInBackground opening socket");
 	   try {
 		   	Log.e("OpenSocketTask", " doInBackground trying to open socket");
 		  	mSocket = new Socket(server, port);
 		  	LOG.info("OpenSocketTask socket " + mSocket);
 		  	Log.e("OpenSocketTask", "opened socket " + mSocket);
		} catch (UnknownHostException e) {
			 Log.d("teste", " trying to create a new socket");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return null;
    }
	
	
    /**
     * 
     */
	@Override
    protected void onPostExecute(Void time) {
    	Log.e("OpenSocketTask", "returning socket " + mSocket);
    	LOG.debug("OpenSocketTask returning socket "  +mSocket);

    	parent.setmSocket(mSocket);
    }
}