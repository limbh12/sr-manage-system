package com.srmanagement.util;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class CryptoUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    @Value("${encryption.secret}")
    private String secretKey;

    private static CryptoUtil instance;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static String encrypt(String data) {
        if (instance == null) {
            throw new IllegalStateException("CryptoUtil not initialized");
        }
        return instance.encryptInternal(data);
    }

    public static String decrypt(String encryptedData) {
        if (instance == null) {
            throw new IllegalStateException("CryptoUtil not initialized");
        }
        return instance.decryptInternal(encryptedData);
    }

    private SecretKeySpec getKeySpec() {
        byte[] key = secretKey.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = new byte[32]; // 256 bit
        int length = Math.min(key.length, keyBytes.length);
        System.arraycopy(key, 0, keyBytes, 0, length);
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    private String encryptInternal(String data) {
        if (data == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getKeySpec());
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    private String decryptInternal(String encryptedData) {
        if (encryptedData == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, getKeySpec());
            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 복호화 실패 시 원본 데이터 반환 (기존 평문 데이터 호환성 또는 디버깅 용도)
            // 실제 운영 환경에서는 보안 정책에 따라 로그를 남기고 예외를 던지거나 null을 반환해야 할 수 있음
            System.err.println("Decryption failed for data: " + e.getMessage());
            return encryptedData;
        }
    }
}
