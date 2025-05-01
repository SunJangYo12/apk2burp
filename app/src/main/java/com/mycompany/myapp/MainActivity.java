package com.mycompany.myapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.net.VpnService;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.*;

public class MainActivity extends Activity 
{
    private static final int VPN_REQUEST_CODE = 0x0F;
    private PacketReceiver packetReceiver;
    private ScrollView tscroll;
    private TextView console;
    private Button btnLog;
    private Boolean logIp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        console = (TextView) findViewById(R.id.subdomner_console);
        tscroll = (ScrollView) findViewById(R.id.subdomner_tscroll);
        btnLog = (Button) findViewById(R.id.change_log);
        btnLog.setText("change log: ip");

        btnLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (logIp) {
                    logIp = false;
                    btnLog.setText("change log: HTTP(80)");
                }
                else {
                    logIp = true;
                    btnLog.setText("change log: ip");
                }

                Toast.makeText(MainActivity.this, "Change log output", Toast.LENGTH_LONG).show();
            }
        });
        

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
            String http = intent.getStringExtra("http");

            if (ip != null && logIp) {
                
                ProcNetReader reader = new ProcNetReader(getApplicationContext());
                
                String[] dat = ip.split(":");
                
                try {
                    
                    String appName = reader.findAppNameForIpPort(dat[0], Integer.parseInt(dat[1]));
                    console.append("Packet to: "+ip+" ("+appName+")\n");
                    
                }catch(Exception e){
                    console.append("Packet to: "+ip+"\n");
                    
                }
            }

            if (!logIp) {
                console.append(http+"\n");
            }

            tscroll.post(new Runnable() {
                @Override
                public void run() {
                    tscroll.fullScroll(View.FOCUS_DOWN);
                }
            });
        }
    }
}

