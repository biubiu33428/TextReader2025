package com.my.textreader.util;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

public class EncryptionUtil {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";
    
    // 固定密钥（在实际应用中，这应该存储在更安全的地方）
    private static final String SECRET_KEY = "TextReaderSecKey";
    
    /**
     * 加密文本内容
     */
    public static String encrypt(String plainText) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(getKey(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 解密文本内容
     */
    public static String decrypt(String encryptedText) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(getKey(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            
            byte[] decodedBytes = Base64.decode(encryptedText, Base64.DEFAULT);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 生成固定的密钥
     */
    private static byte[] getKey() {
        try {
            // 使用固定字符串生成16字节密钥
            String key = SECRET_KEY;
            if (key.length() < 16) {
                // 如果密钥长度不足16位，用0填充
                StringBuilder sb = new StringBuilder(key);
                while (sb.length() < 16) {
                    sb.append("0");
                }
                key = sb.toString();
            } else if (key.length() > 16) {
                // 如果密钥长度超过16位，截取前16位
                key = key.substring(0, 16);
            }
            return key.getBytes("UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 验证是否为加密文件的文件头
     */
    public static boolean isEncryptedFile(String content) {
        // 检查是否以特定标识开头
        return content != null && content.startsWith("TEXTREADER_ENCRYPTED:");
    }
    
    /**
     * 为加密内容添加文件头标识
     */
    public static String addEncryptedHeader(String encryptedContent) {
        return "TEXTREADER_ENCRYPTED:" + encryptedContent;
    }
    
    /**
     * 移除加密文件头标识
     */
    public static String removeEncryptedHeader(String content) {
        if (isEncryptedFile(content)) {
            return content.substring("TEXTREADER_ENCRYPTED:".length());
        }
        return content;
    }
} 