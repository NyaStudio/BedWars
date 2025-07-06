package cn.nekopixel.bedwars.auth;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class CryptoUtil {
    
    private static KeyPair rsaKeyPair;
    private static SecretKey aesKey;

    private static final String SERVER_PUBLIC_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAt8ldOVEA8/T7OdzTU5Po" +
                    "wNgrZ2N7K2/HinyvcdTolL4l8Hbph750V60BWP9uuE1oHLrYm4dl7COwQqJZjtlj" +
                    "c2aIggh3XUTAlF9oDBZ6mDIWJPJq/Ct5xCEGQ9nF39fp3lYKDNL0tXnMTxgSa1G2" +
                    "xINDp9zPIRqgRJACI6E6nDEsm7pIaKOCRlQ3Shlb7i8FUAm27SVeAO75vaU27aOt" +
                    "T18EFqy90bX2V2KZVOIbo/BVBsCUrzk0RYTxSKn1DDh4IptUP+0rosL1RJHgRB4O" +
                    "cMJBdEFKyOfal38ebPYMLq7aK1LvadGemTrkkNUYV7vZiyhoSzek/RvliMnPOXij" +
                    "LQIDAQAB";
    
    static {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            rsaKeyPair = keyGen.generateKeyPair();
            
            KeyGenerator aesGen = KeyGenerator.getInstance("AES");
            aesGen.init(256);
            aesKey = aesGen.generateKey();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getClientPublicKey() {
        return Base64.getEncoder().encodeToString(rsaKeyPair.getPublic().getEncoded());
    }

    public static String encryptAESKey() throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(SERVER_PUBLIC_KEY);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey serverPublicKey = keyFactory.generatePublic(spec);
        
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
        byte[] encryptedKey = cipher.doFinal(aesKey.getEncoded());
        
        return Base64.getEncoder().encodeToString(encryptedKey);
    }

    public static String encryptData(String data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);
        byte[] encrypted = cipher.doFinal(data.getBytes("UTF-8"));
        
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        
        return Base64.getEncoder().encodeToString(combined);
    }

    public static String decryptData(String encryptedData) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedData);
        
        byte[] iv = new byte[16];
        byte[] encrypted = new byte[combined.length - 16];
        System.arraycopy(combined, 0, iv, 0, 16);
        System.arraycopy(combined, 16, encrypted, 0, encrypted.length);
        
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);
        byte[] decrypted = cipher.doFinal(encrypted);
        
        return new String(decrypted, "UTF-8");
    }

    public static String signData(String data) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(rsaKeyPair.getPrivate());
        signature.update(data.getBytes("UTF-8"));
        
        byte[] signBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signBytes);
    }

    public static boolean verifyServerSignature(String data, String signatureStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(SERVER_PUBLIC_KEY);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey serverPublicKey = keyFactory.generatePublic(spec);
        
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(serverPublicKey);
        signature.update(data.getBytes("UTF-8"));
        
        byte[] signBytes = Base64.getDecoder().decode(signatureStr);
        return signature.verify(signBytes);
    }

    public static long generateTimestamp() {
        return System.currentTimeMillis();
    }
    public static boolean isTimestampValid(long timestamp) {
        long current = System.currentTimeMillis();
        return Math.abs(current - timestamp) < 5 * 60 * 1000;
    }

    public static String generateNonce() {
        byte[] nonce = new byte[32];
        new SecureRandom().nextBytes(nonce);
        return Base64.getEncoder().encodeToString(nonce);
    }
}