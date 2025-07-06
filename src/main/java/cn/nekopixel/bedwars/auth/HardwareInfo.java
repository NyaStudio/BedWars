package cn.nekopixel.bedwars.auth;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.Enumeration;

public class HardwareInfo {
    public static String getFingerprint() {
        try {
            String cpuId = getCPUId();
            String macAddress = getMACAddress();
            String motherboardId = getMotherboardId();
            String diskId = getDiskId();
            
            String combined = cpuId + "|" + macAddress + "|" + motherboardId + "|" + diskId;
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            return getFallbackFingerprint();
        }
    }

    private static String getCPUId() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String cpuId = "";
            
            if (os.contains("win")) {
                Process process = Runtime.getRuntime().exec("wmic cpu get ProcessorId");
                process.getOutputStream().close();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty() && !line.contains("ProcessorId")) {
                        cpuId = line.trim();
                        break;
                    }
                }
                reader.close();
            } else if (os.contains("linux")) {
                Process process = Runtime.getRuntime().exec("cat /proc/cpuinfo");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Serial") || line.contains("Hardware")) {
                        String[] parts = line.split(":");
                        if (parts.length > 1) {
                            cpuId = parts[1].trim();
                            break;
                        }
                    }
                }
                reader.close();
            }
            
            return cpuId.isEmpty() ? "NO_CPU_ID" : cpuId;
        } catch (Exception e) {
            return "CPU_ERROR";
        }
    }

    private static String getMACAddress() {
        try {
            StringBuilder sb = new StringBuilder();
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                if (!ni.isLoopback() && !ni.isVirtual() && ni.isUp()) {
                    byte[] mac = ni.getHardwareAddress();
                    if (mac != null && mac.length > 0) {
                        for (int i = 0; i < mac.length; i++) {
                            sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                        }
                        break;
                    }
                }
            }
            
            return sb.length() > 0 ? sb.toString() : "NO_MAC";
        } catch (Exception e) {
            return "MAC_ERROR";
        }
    }

    private static String getMotherboardId() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String motherboardId = "";
            
            if (os.contains("win")) {
                Process process = Runtime.getRuntime().exec("wmic baseboard get SerialNumber");
                process.getOutputStream().close();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty() && !line.contains("SerialNumber")) {
                        motherboardId = line.trim();
                        break;
                    }
                }
                reader.close();
            } else if (os.contains("linux")) {
                Process process = Runtime.getRuntime().exec("sudo dmidecode -s baseboard-serial-number");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                motherboardId = reader.readLine();
                reader.close();
            }
            
            return motherboardId == null || motherboardId.isEmpty() ? "NO_MB_ID" : motherboardId;
        } catch (Exception e) {
            return "MB_ERROR";
        }
    }

    private static String getDiskId() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String diskId = "";
            
            if (os.contains("win")) {
                Process process = Runtime.getRuntime().exec("wmic diskdrive get SerialNumber");
                process.getOutputStream().close();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty() && !line.contains("SerialNumber")) {
                        diskId = line.trim();
                        break;
                    }
                }
                reader.close();
            } else if (os.contains("linux")) {
                Process process = Runtime.getRuntime().exec("lsblk -no UUID");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                diskId = reader.readLine();
                reader.close();
            }
            
            return diskId == null || diskId.isEmpty() ? "NO_DISK_ID" : diskId;
        } catch (Exception e) {
            return "DISK_ERROR";
        }
    }

    private static String getFallbackFingerprint() {
        try {
            String userName = System.getProperty("user.name");
            String osName = System.getProperty("os.name");
            String osArch = System.getProperty("os.arch");
            String javaVendor = System.getProperty("java.vendor");
            
            String combined = userName + osName + osArch + javaVendor;
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            return "FALLBACK_ERROR";
        }
    }
}