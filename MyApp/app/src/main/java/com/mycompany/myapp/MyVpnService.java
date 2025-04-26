package com.mycompany.myapp;
import android.net.*;
import android.os.*;
import android.content.*;
import java.io.*;
import java.nio.*;
import android.util.*;

public class MyVpnService extends VpnService {

    private ParcelFileDescriptor tunInterface;
    private Thread vpnThread;
	
	private String lastIp = null;
	private int lastPort = -1;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Builder builder = new Builder();

        // Buat interface TUN
        tunInterface = builder
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .setSession("MyVPN")
            .establish();

        vpnThread = new Thread(new Runnable() {
				@Override
				public void run() {
					runVpn();
				}
			});
        vpnThread.start();

        return START_STICKY;
    }
	
	
	
	private String parseDestinationIpAndPort(byte[] packet, int length) {
		if (length < 20 || (packet[0] >> 4) != 4) return "Invalid IPv4";

		// IP Destination Address
		int destIp1 = packet[16] & 0xFF;
		int destIp2 = packet[17] & 0xFF;
		int destIp3 = packet[18] & 0xFF;
		int destIp4 = packet[19] & 0xFF;

		String ip = destIp1 + "." + destIp2 + "." + destIp3 + "." + destIp4;

		int ipHeaderLength = (packet[0] & 0x0F) * 4;
		if (length < ipHeaderLength + 4) return ip + ":???"; // data terlalu pendek

		int destPort = ((packet[ipHeaderLength + 2] & 0xFF) << 8) | (packet[ipHeaderLength + 3] & 0xFF);
		
		lastIp = ip;
		lastPort = destPort;

		return ip + ":" + destPort;
	}
	

    private void runVpn() {
		FileInputStream in = new FileInputStream(tunInterface.getFileDescriptor());
		ByteBuffer buffer = ByteBuffer.allocate(32767);
		
		while (true) {
			try {
				int length = in.read(buffer.array());
				if (length > 0) {
					byte[] packetBytes = buffer.array();
					String destIp = parseDestinationIpAndPort(packetBytes,length);

					Log.d("VPN", "Packet to: " + destIp);

					Intent intent = new Intent("com.example.myvpn.PACKET_COUNT");
					intent.putExtra("ip", destIp);
					sendBroadcast(intent);
					
					
				}
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}
	}

    @Override
    public void onDestroy() {
        try {
            if (tunInterface != null) tunInterface.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (vpnThread != null) vpnThread.interrupt();
    }
}
