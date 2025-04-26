package com.mycompany.myapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.*;

public class MainActivity extends Activity 
{
	private static final int VPN_REQUEST_CODE = 0x0F;
	private EditText statusTextView;
	private PacketReceiver packetReceiver;
	private StringBuilder logBuilder = new StringBuilder();
	
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		statusTextView = new EditText(this);
		statusTextView.setFocusable(false);
		statusTextView.setFocusableInTouchMode(false);
		statusTextView.setClickable(false);
		
		statusTextView.setText("Packets received: 0");
		setContentView(statusTextView);

		packetReceiver = new PacketReceiver();
		registerReceiver(packetReceiver, new IntentFilter("com.example.myvpn.PACKET_COUNT"));

		Intent intent = VpnService.prepare(this);
		if (intent != null) {
			startActivityForResult(intent, VPN_REQUEST_CODE);
		} else {
			onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
			Intent vpnIntent = new Intent(this, MyVpnService.class);
			startService(vpnIntent);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(packetReceiver);
	}

	// Receiver untuk update UI
	private class PacketReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			
			String ip = intent.getStringExtra("ip");
			if (ip != null) {
				
				ProcNetReader reader = new ProcNetReader(getApplicationContext());
				
				String[] dat = ip.split(":");
				
				try {
					
					String appName = reader.findAppNameForIpPort(dat[0], Integer.parseInt(dat[1]));
					logBuilder.append("Packet to: ").append(ip).append(" ("+appName+")").append("\n");
					
				}catch(Exception e){
					logBuilder.append("Packet to: ").append(ip).append("\n");
					
				}
				
				
				 
				statusTextView.setText(logBuilder.toString());
			}
		}
	}
}

