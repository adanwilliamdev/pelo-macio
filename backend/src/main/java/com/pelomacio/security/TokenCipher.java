package com.pelomacio.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifra segredos guardados no banco (refresh tokens do Google) com AES-256-GCM.
 * Formato: Base64(iv[12] || texto cifrado || tag). A chave é derivada (SHA-256) de {@code pelomacio.security.token-key};
 * se ela estiver vazia, usa o segredo JWT. (Não dá para resolver isso com valor padrão no placeholder porque o
 * docker-compose repassa a variável como string vazia, e vazio é um valor "definido" para o Spring.)
 */
@Component
public class TokenCipher {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec chave;
    private final SecureRandom random = new SecureRandom();

    public TokenCipher(@Value("${pelomacio.security.token-key:}") String chaveDedicada,
                       @Value("${pelomacio.jwt.secret}") String segredoJwt) {
        String segredo = chaveDedicada == null || chaveDedicada.isBlank() ? segredoJwt : chaveDedicada;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(segredo.getBytes(StandardCharsets.UTF_8));
            this.chave = new SecretKeySpec(hash, "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    public String cifrar(String texto) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, chave, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] saida = new byte[IV_BYTES + cifrado.length];
            System.arraycopy(iv, 0, saida, 0, IV_BYTES);
            System.arraycopy(cifrado, 0, saida, IV_BYTES, cifrado.length);
            return Base64.getEncoder().encodeToString(saida);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao cifrar", e);
        }
    }

    /** @throws IllegalStateException se o valor estiver corrompido ou a chave tiver mudado. */
    public String decifrar(String base64) {
        try {
            byte[] entrada = Base64.getDecoder().decode(base64);
            if (entrada.length <= IV_BYTES) throw new IllegalStateException("Valor cifrado inválido");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, chave, new GCMParameterSpec(TAG_BITS, entrada, 0, IV_BYTES));
            return new String(cipher.doFinal(entrada, IV_BYTES, entrada.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Não foi possível decifrar (a chave mudou?)", e);
        }
    }
}
