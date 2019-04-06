/*
 * Copyright (C) 2019 nift4
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package org.nift4.btdemo;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.bluetooth.BluetoothDevice;
import java.util.UUID;
import android.widget.Button;
import android.view.View;
import org.nift4.btlib.BluetoothService;
import android.app.AlertDialog;
import android.os.Handler;
import android.content.DialogInterface;

public class MainActivity extends Activity 
{
	// Bluetooth Service
	BluetoothService mBluetoothService;
	// Was connected in this session
	private boolean wasConnected = false;
	// UUIDs
	private String insecUuid = "071cb722-6728-4fb4-92a1-1ec0bcbee21e";
	private String secUuid = "53fcc9ce-6227-470b-935f-248efc3b3968";
	
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		// Create handlers to use h.postDelay()
		final Handler h = new Handler(this.getMainLooper());
		try {
			// Create Bluetooth Service
			mBluetoothService = new BluetoothService(this, UUID.fromString(insecUuid), UUID.fromString(secUuid), new BluetoothService.Callback(){
				// Bluetooth Enabled Callback
					@Override
					public void callback()
					{
						// Add functions to the butzons
						((Button) findViewById(R.id.server_btn)).setOnClickListener(new View.OnClickListener(){
								@Override
								public void onClick(View p1)
								{
									onServerStart();
								}
							});
						((Button) findViewById(R.id.client_btn)).setOnClickListener(new View.OnClickListener(){
								@Override
								public void onClick(View p1)
								{
									onClientStart();
								}
							});
						((Button) findViewById(R.id.lsnews_btn)).setOnClickListener(new View.OnClickListener(){
								@Override
								public void onClick(View p1)
								{
									// Get BtData object
									// buffer is byte[]
									// bytes is count of the usable data bytes in buffer
									BluetoothService.BtData d = mBluetoothService.read();
									if (d != null) {
										new AlertDialog.Builder(MainActivity.this)
											.setMessage(new String(d.buffer,0,d.bytes))
											.setCancelable(true)
											.show();
									}
								}
							});
						// Wait 500ms
						h.postDelayed(new Runnable(){
							@Override
							public void run()
							{
								boolean nodelay = false;
								// If state is connected for the first time
								if (mBluetoothService.getState() == mBluetoothService.STATE_CONNECTED && !wasConnected) {
									wasConnected = true;
									new AlertDialog.Builder(MainActivity.this)
										. setCancelable(true)
										. setNeutralButton("Ok", new DialogInterface.OnClickListener(){
											@Override
											public void onClick(DialogInterface p1, int p2)
											{
												p1.dismiss();
											}
										})
										. setMessage("Connected")
										. setTitle("Bluetooth")
										. show();
								// If connection lost
								} else if (mBluetoothService.getState() == mBluetoothService.STATE_NONE && mBluetoothService.getNoneReason() == mBluetoothService.NONE_REASON_CONN_LOST) {
									new AlertDialog.Builder(MainActivity.this)
									. setCancelable(false)
									. setNegativeButton("Quit", new DialogInterface.OnClickListener(){
										@Override
										public void onClick(DialogInterface p1, int p2)
										{
											p1.dismiss();
											MainActivity.this.finish();
										}
									})
									. setMessage("Connection lost")
									. setTitle("Bluetooth Error")
									. show();
									// "break;" this loop
									nodelay = true;
								}
								if(!nodelay){
									h.postDelayed(this,500);
								}
							}
						},500);
					}
				}, new BluetoothService.Callback(){
					// Enable failed
					@Override
					public void callback()
					{
						MainActivity.this.finish();
					}
			});
		} catch (BluetoothService.BluetoothDisabledException e) {
			e.printStackTrace();
			finish();
		} catch (BluetoothService.NoBluetoothAdapterFoundException e) {
			e.printStackTrace();
			finish();
		}
    }
	private void onClientStart(){
		// View device selector
		mBluetoothService.showDeviceSelector();
		// See onActivityResult()
	}
	private void onServerStart() {
		// Make discoverable
		mBluetoothService.makeDiscoverable();
		// Start listener
		mBluetoothService.startServer();
		// Create time helper
		final Handler h = new Handler(this.getMainLooper());
		// Run after 1000ms
		h.postDelayed(new Runnable(){
				@Override
				public void run()
				{
					// If connected
					if (mBluetoothService.getState() == mBluetoothService.STATE_CONNECTED) {
						mBluetoothService.write("Hello from Server!".getBytes());
						// Send other message after 1s
						new Handler(MainActivity.this.getMainLooper()).postDelayed(new Runnable(){
								@Override
								public void run()
								{
									mBluetoothService.write("hellohl222".getBytes());
								}
							},1000);
						// Else wait 1s and check again
					} else {
						h.postDelayed(this,1000);
					}
				}
		},1000);
	}
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Get device selector result
		// IMPORTANT: Add this call always!! It's needed to enable Bluetooth
		BluetoothDevice device = mBluetoothService.getDeviceSelectorResult(requestCode,resultCode,data);
		// If Device Selector returned
		if (device != null) {
			// Create another handler
			final Handler h = new Handler(this.getMainLooper());
			// Try connection
			mBluetoothService.connect(device,true);
			// Run after delay
			h.postDelayed(new Runnable(){
					@Override
					public void run()
					{
						// If connected run this
						if (mBluetoothService.getState() == mBluetoothService.STATE_CONNECTED) {
							// View connected dialog
							new AlertDialog.Builder(MainActivity.this)
								. setCancelable(true)
								. setNeutralButton("Ok", new DialogInterface.OnClickListener(){
									@Override
									public void onClick(DialogInterface p1, int p2)
									{
										p1.dismiss();
									}
								})
								. setMessage("Connected")
								. setTitle("Bluetooth")
								. show();
							// Run after delay (500ms)
							h.postDelayed(new Runnable() {
								@Override
								public void run()
								{
									// If connection lost
									if (mBluetoothService.getState() == mBluetoothService.STATE_NONE && mBluetoothService.getNoneReason() == mBluetoothService.NONE_REASON_CONN_LOST) {
										new AlertDialog.Builder(MainActivity.this)
											. setCancelable(false)
											. setNegativeButton("Quit", new DialogInterface.OnClickListener(){
												@Override
												public void onClick(DialogInterface p1, int p2)
												{
													p1.dismiss();
													MainActivity.this.finish();
												}
											})
											. setMessage("Connection lost")
											. setTitle("Bluetooth Error")
											. show();
									// If new message
									} else if (mBluetoothService.getState() == mBluetoothService.STATE_CONNECTED && mBluetoothService.newMessageAvailable()) {
										// Send text
										mBluetoothService.write("Hello back from Client!".getBytes());
										// Send more data afetr delay
										h.postDelayed(new Runnable(){
												@Override
												public void run()
												{
													mBluetoothService.write("all2".getBytes());
												}
											}, 1000);
										// If not connection lost or new message, run this if in 500ms again
									} else {
										h.postDelayed(this,500);
									}
								}
							}, 500);
						// If connection failed
						} else if (mBluetoothService.getState() == mBluetoothService.STATE_NONE && mBluetoothService.getNoneReason() == mBluetoothService.NONE_REASON_CONN_FAIL) {
							new AlertDialog.Builder(MainActivity.this)
								. setCancelable(false)
								. setNegativeButton("Quit", new DialogInterface.OnClickListener(){
									@Override
									public void onClick(DialogInterface p1, int p2)
									{
										p1.dismiss();
										MainActivity.this.finish();
									}
								})
								. setMessage("Connection failed")
								. setTitle("Bluetooth Error")
								. show();
						// Else run this 1000 ms later to wait for connection result
						} else {
							h.postDelayed(this,1000);
						}
					}
			},1000);
		} else {
			// Other activityResult
		}
	}
}
