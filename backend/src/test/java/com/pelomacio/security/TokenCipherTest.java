package com.pelomacio.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TokenCipherTest {

    @Test
    void cifraEDecifra() {
        TokenCipher cipher = new TokenCipher("chave-de-teste", "segredo-jwt");
        String cifrado = cipher.cifrar("1//refresh-token-secreto");

        assertNotEquals("1//refresh-token-secreto", cifrado);
        assertFalse(cifrado.contains("refresh-token"));
        assertEquals("1//refresh-token-secreto", cipher.decifrar(cifrado));
    }

    @Test
    void cadaCifragemUsaUmIvDiferente() {
        TokenCipher cipher = new TokenCipher("chave-de-teste", "segredo-jwt");
        assertNotEquals(cipher.cifrar("x"), cipher.cifrar("x"));
    }

    @Test
    void chaveDiferenteNaoDecifra() {
        String cifrado = new TokenCipher("chave-a", "jwt").cifrar("segredo");
        TokenCipher outra = new TokenCipher("chave-b", "jwt");
        assertThrows(IllegalStateException.class, () -> outra.decifrar(cifrado));
    }

    @Test
    void chaveVaziaUsaOSegredoJwt() {
        String cifrado = new TokenCipher("", "segredo-jwt").cifrar("abc");
        assertEquals("abc", new TokenCipher(null, "segredo-jwt").decifrar(cifrado));
    }

    @Test
    void valorAdulteradoEhRejeitado() {
        TokenCipher cipher = new TokenCipher("chave", "jwt");
        assertThrows(IllegalStateException.class, () -> cipher.decifrar("nao-e-base64-valido!!"));
    }
}
