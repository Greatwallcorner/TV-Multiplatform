package com.corner.quickjs.utils;

import com.corner.catvodcore.util.Utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

public class Crypto {

    public static String md5(String text) {
        try {
            return Utils.md5(text);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String aes(String mode, boolean encrypt, String input, boolean inBase64, String key, String iv, boolean outBase64) {
        try {
            byte[] keyBuf = key.getBytes();
            if (keyBuf.length < 16) keyBuf = Arrays.copyOf(keyBuf, 16);
            byte[] ivBuf = iv == null ? new byte[0] : iv.getBytes();
            if (ivBuf.length < 16) ivBuf = Arrays.copyOf(ivBuf, 16);
            Cipher cipher = Cipher.getInstance(mode + "Padding");
            SecretKeySpec keySpec = new SecretKeySpec(keyBuf, "AES");
            if (iv == null) cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, keySpec);
            else cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(ivBuf));
            byte[] inBuf = inBase64 ? Utils.INSTANCE.decode(input) : input.getBytes("UTF-8");
            return outBase64 ? Utils.INSTANCE.base64(cipher.doFinal(inBuf)) : new String(cipher.doFinal(inBuf), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String rsa(String mode, boolean pub, boolean encrypt, String input, boolean inBase64, String key, boolean outBase64) {
        try {
            Key rsaKey = generateKey(pub, key);
            int len = getModulusLength(rsaKey);
            byte[] outBytes = new byte[0];
            byte[] inBytes = inBase64 ? Utils.INSTANCE.decode(input) : input.getBytes("UTF-8");
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, rsaKey);
            int blockLen = encrypt ? len / 8 - 11 : len / 8;
            int bufIdx = 0;
            while (bufIdx < inBytes.length) {
                int bufEndIdx = Math.min(bufIdx + blockLen, inBytes.length);
                byte[] tmpInBytes = new byte[bufEndIdx - bufIdx];
                System.arraycopy(inBytes, bufIdx, tmpInBytes, 0, tmpInBytes.length);
                byte[] tmpBytes = cipher.doFinal(tmpInBytes);
                bufIdx = bufEndIdx;
                outBytes = concatArrays(outBytes, tmpBytes);
            }
            return outBase64 ? Utils.INSTANCE.base64(outBytes) : new String(outBytes, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private static Key generateKey(boolean pub, String key) throws Exception {
        if (pub) key = key.replaceAll(System.lineSeparator(), "").replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");
        else key = key.replaceAll(System.lineSeparator(), "").replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "");
        return pub ? KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Utils.INSTANCE.base64(key).getBytes())) : KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Utils.INSTANCE.decode(key)));
    }

    private static int getModulusLength(Key key) {
        if (key instanceof PublicKey) return ((RSAPublicKey) key).getModulus().bitLength();
        else return ((RSAPrivateKey) key).getModulus().bitLength();
    }

    private static byte[] concatArrays(byte[] a, byte[] b) {
        int aLen = a.length;
        int bLen = b.length;
        byte[] result = new byte[aLen + bLen];
        System.arraycopy(a, 0, result, 0, aLen);
        System.arraycopy(b, 0, result, aLen, bLen);
        return result;
    }
}
