package com.mycompany.myapp;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ProcNetReader {

    private Context context;

    public ProcNetReader(Context context) {
        this.context = context;
    }

    public String findAppNameForIpPort(String destIp, int destPort) {
        String hexIp = ipToHex(destIp);
        String hexPort = Integer.toHexString(destPort).toUpperCase();
        if (hexPort.length() < 4) {
            hexPort = "0" + hexPort; // Pastikan 4 digit
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/proc/net/tcp"));
            String line;
            reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                String[] parts = line.split("\\s+");
                if (parts.length < 10) continue;

                String localAddress = parts[1];
                String remoteAddress = parts[2];
                int uid = Integer.parseInt(parts[7]);

                // remoteAddress format: IP:PORT (hex)
                String[] remoteParts = remoteAddress.split(":");
                if (remoteParts.length != 2) continue;

                String remoteIp = remoteParts[0];
                String remotePort = remoteParts[1];

                if (remoteIp.equals(hexIp) && remotePort.equals(hexPort)) {
                    return getAppNameFromUid(uid);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) try { reader.close(); } catch (IOException ignored) {}
        }
        return null;
    }

    private String getAppNameFromUid(int uid) {
        PackageManager pm = context.getPackageManager();
        String[] packages = pm.getPackagesForUid(uid);
        if (packages != null && packages.length > 0) {
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(packages[0], 0);
                return pm.getApplicationLabel(appInfo).toString();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return "Unknown";
    }

    private String ipToHex(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return "00000000";
        StringBuilder hex = new StringBuilder();
        for (int i = 3; i >= 0; i--) { // Little-endian
            int part = Integer.parseInt(parts[i]);
            String h = Integer.toHexString(part).toUpperCase();
            if (h.length() == 1) hex.append("0");
            hex.append(h);
        }
        return hex.toString();
    }
}
