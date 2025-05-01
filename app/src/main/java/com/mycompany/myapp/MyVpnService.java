package com.mycompany.myapp;

import android.net.*;
import android.os.*;
import android.content.*;
import java.io.*;
import java.nio.*;
import java.util.Arrays;
import android.util.*;
import java.nio.charset.*;
import java.net.*;

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

    private String send2burpsuite(byte[] packet, int length)
    {
        /*int version = (packet[0] >> 4) & 0xF;

        //Ambil versi IP (harusnya IPv4 = 4)
        if (version != 4) return "versi IP: "+version;

        //Ambil header length(IP)
        int ihl = packet[0] & 0x0F;
        int ipHeaderLength = ihl * 4;

        //Cek protocol (harus TCP = 6)
        int protocol = packet[9] & 0xFF;
        if (protocol != 6) return "Protocol: "+protocol;

        //Ambil ip tujuan
        String destIp = (packet[16] & 0xFF) + "." + (packet[17] & 0xFF) + "." + (packet[18] & 0xFF) + "." + (packet[19] & 0xFF);
        //Ambil port tujuan
        int destPort = ((packet[ipHeaderLength+2] & 0xFF) << 8) | (packet[ipHeaderLength+3] & 0xFF);

        //Cek apakah HTTP(port 80)
        if (destPort != 80) return "destination: "+destIp+":"+destPort;

        //Ambil payload HTTP
        int tcpHeaderOffset = ipHeaderLength;
        int dataOffset = ((packet[tcpHeaderOffset+12] >> 4) & 0xF) * 4;

        int payloadOffset = ipHeaderLength + dataOffset;
        int payloadLength = length - payloadOffset;

        if (payloadLength <= 0) return "payload < 0";

        byte[] httpPayload = Arrays.copyOfRange(packet, payloadOffset, length);

        String httpRequest = new String(httpPayload, StandardCharsets.UTF_8);

        Log.d("apk2burp", "HTTP Request:\n"+httpRequest);
		
        if (false) return "HTTP request: "+httpRequest;
        
        try {
            Socket proxySocket = new Socket("192.168.0.102", 8080);
            OutputStream proxyOut = proxySocket.getOutputStream();
            InputStream proxyIn = proxySocket.getInputStream();

            proxyOut.write(httpPayload);
            proxyOut.flush();

            // Baca response dari Burp
            byte[] responseBuffer = new byte[8192];
            int bytesRead = proxyIn.read(responseBuffer);

            if (bytesRead > 0) {
                // Di sini kamu bisa bungkus balik jadi TCP packet ke app via `tun0`
                // Tapi untuk demo, cukup log dulu
                String response = new String(responseBuffer, 0, bytesRead, StandardCharsets.UTF_8);
                Log.i("VPN", "Response from Burp:\n" + response);
            }

            proxySocket.close();

            return "success send HTTP to burpsuite";
        }
        catch(Exception e)
        {
            return "error send HTTP to burpsuite";
        }*/
        int version = (packet[0] >> 4) & 0xF;
        if (version != 4) return "versi IP: " + version;

        int ihl = packet[0] & 0x0F;
        int ipHeaderLength = ihl * 4;

        int protocol = packet[9] & 0xFF;
        if (protocol != 6) return "Protocol: " + protocol;

        String destIp = (packet[16] & 0xFF) + "." + (packet[17] & 0xFF) + "." +
                        (packet[18] & 0xFF) + "." + (packet[19] & 0xFF);

        int destPort = ((packet[ipHeaderLength + 2] & 0xFF) << 8) | (packet[ipHeaderLength + 3] & 0xFF);
        if (destPort != 80) return "destination: " + destIp + ":" + destPort;

        // --- Debug: log awal paket ---
        StringBuilder hexDump = new StringBuilder();
        for (int i = 0; i < Math.min(length, 64); i++) {
            int b = packet[i] & 0xFF;
            char c = (b >= 32 && b <= 126) ? (char) b : '.';
            //hexDump.append(String.format("%02X (%c) ", b, c));
            hexDump.append(String.format("(%c) ", c));
        }

        StringBuilder outx = new StringBuilder();
        outx.append("Packet length: " + length+"\n");
        outx.append("Dest IP: " + destIp+":"+destPort+"\n");
        outx.append("IP header length: " + ipHeaderLength+"\n");
        outx.append("TCP header offset: " + ipHeaderLength+"\n");
        outx.append("Data offset (TCP): " + (((packet[ipHeaderLength + 12] >> 4) & 0xF) * 4)+"\n");

        int dataOffset = ((packet[ipHeaderLength + 12] >> 4) & 0xF) * 4;
        int payloadOffset = ipHeaderLength + dataOffset;
        int payloadLength = length - payloadOffset;

        outx.append("Payload offset: " + payloadOffset+"\n");
        outx.append("Payload length: " + payloadLength+"\n");
        outx.append("Packet (hex+ascii):\n" + hexDump.toString()+"\n");

        //if (payloadLength <= 0) return "payload < 0";

        byte[] httpPayload = Arrays.copyOfRange(packet, payloadOffset, length);
        String httpRequest = new String(httpPayload, StandardCharsets.UTF_8);

        //Log.d("apk2burp", "HTTP Request:\n" + httpRequest);
        return outx.toString();
    }
    

    private void runVpn() {
        FileInputStream in = new FileInputStream(tunInterface.getFileDescriptor());
        byte[] buffer = new byte[32767];

        
        while (true) {
            try {
                int length = in.read(buffer);

                if (length > 0) {
                    //String destIp = parseDestinationIpAndPort(buffer, length);

                    String httpRequest = send2burpsuite(buffer, length);



                    Intent intent = new Intent("com.example.myvpn.PACKET_COUNT");
                    //intent.putExtra("ip", destIp);
                    intent.putExtra("http", httpRequest);
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
