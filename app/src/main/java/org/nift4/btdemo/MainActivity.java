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
	BluetoothService mBluetoothService;
	private boolean wasConnected = false;

	private String insecUuid = "071cb722-6728-4fb4-92a1-1ec0bcbee21e";
	private String secUuid = "53fcc9ce-6227-470b-935f-248efc3b3968";
	
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		final Handler h = new Handler(this.getMainLooper());
		try {
			mBluetoothService = new BluetoothService(this, UUID.fromString(insecUuid), UUID.fromString(secUuid), new BluetoothService.Callback(){
					@Override
					public void callback()
					{
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
									BluetoothService.BtData d = mBluetoothService.read();
									if (d != null) {
										new AlertDialog.Builder(MainActivity.this)
											.setMessage(new String(d.buffer,0,d.bytes))
											.setCancelable(true)
											.show();
									}
								}
							});
						h.postDelayed(new Runnable(){
							@Override
							public void run()
							{
								boolean nodelay = false;
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
									nodelay = true;
								}
								if(!nodelay){
									h.postDelayed(this,500);
								}
							}
						},500);
					}
				}, new BluetoothService.Callback(){
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
		mBluetoothService.showDeviceSelector();
	}
	private void onServerStart() {
		mBluetoothService.makeDiscoverable();
		mBluetoothService.startServer();
		final Handler h = new Handler(this.getMainLooper());
		h.postDelayed(new Runnable(){
				@Override
				public void run()
				{
					if (mBluetoothService.getState() == mBluetoothService.STATE_CONNECTED) {
						mBluetoothService.write("Hello from Server!".getBytes());
						new Handler(MainActivity.this.getMainLooper()).postDelayed(new Runnable(){
								@Override
								public void run()
								{
									mBluetoothService.write("hellohl222".getBytes());
								}
							},1000);
					} else {
						h.postDelayed(this,1000);
					}
				}
		},1000);
	}
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		BluetoothDevice device = mBluetoothService.getDeviceSelectorResult(requestCode,resultCode,data);
		if (device != null) {
			final Handler h = new Handler(this.getMainLooper());
			mBluetoothService.connect(device,true);
			h.postDelayed(new Runnable(){
					@Override
					public void run()
					{
						if (mBluetoothService.getState() == mBluetoothService.STATE_CONNECTED) {
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
							h.postDelayed(new Runnable() {
								@Override
								public void run()
								{
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
									} else if (mBluetoothService.getState() == mBluetoothService.STATE_CONNECTED && mBluetoothService.newMessageAvailable()) {
										mBluetoothService.write("Hello back from Client!".getBytes());
										h.postDelayed(new Runnable(){
												@Override
												public void run()
												{
													mBluetoothService.write("all2".getBytes());
												}
											}, 1000);
									} else {
										h.postDelayed(this,500);
									}
								}
							}, 500);
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
							
						} else {
							h.postDelayed(this,1000);
						}
					}
			},1000);
		}
	}
}
