package com.sfi.matrix.support.dto;

/**
 * 敏感参数值的对外密文载体。
 *
 * <p>对外接口（list/get/value/values）遇到 sensitive=true 的配置，configValue 不是明文字符串，
 * 而是本对象——前端拿到的是结构化密文，无法直接塞进任何明文输入项；改值只能走 /secret 通道
 * 提交明文字符串。这是「类型隔离」：靠结构差异防误操作，而不是靠前缀/格式检测（后者可被
 * 简单改写绕过）。</p>
 *
 * @author matrix
 */
public class SecretCipher {

    /** 固定为 true，标记该值已加密。 */
    private boolean encrypted;
    /** Base64 密文（AES-256-GCM，含随机 IV）。 */
    private String cipher;

    public SecretCipher() {
    }

    public SecretCipher(boolean encrypted, String cipher) {
        this.encrypted = encrypted;
        this.cipher = cipher;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getCipher() {
        return cipher;
    }

    public void setCipher(String cipher) {
        this.cipher = cipher;
    }
}
