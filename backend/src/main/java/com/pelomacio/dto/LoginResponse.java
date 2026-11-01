package com.pelomacio.dto;

import com.pelomacio.model.Loja;
import com.pelomacio.model.Usuario;
import java.util.List;

public record LoginResponse(String token, String nome, String email, List<String> perfis,
                            String lojaNome, String lojaSlug) {

    public static LoginResponse de(String token, Usuario u, Loja loja) {
        return new LoginResponse(token, u.getNome(), u.getEmail(),
                u.getPerfis().stream().map(Enum::name).toList(), loja.getNome(), loja.getSlug());
    }
}
