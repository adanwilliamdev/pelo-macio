package com.pelomacio.controller;

import com.pelomacio.dto.*;
import com.pelomacio.exception.NegocioException;
import com.pelomacio.model.Usuario;
import com.pelomacio.repository.LojaRepository;
import com.pelomacio.repository.UsuarioRepository;
import com.pelomacio.security.JwtService;
import com.pelomacio.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final UsuarioService usuarioService;
    private final LojaRepository lojas;

    public AuthController(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt,
                          UsuarioService usuarioService, LojaRepository lojas) {
        this.lojas = lojas;
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        Usuario u = usuarios.findByEmailIgnoreCase(req.email())
                .filter(x -> encoder.matches(req.senha(), x.getSenha()))
                .orElseThrow(() -> new NegocioException("E-mail ou senha inválidos.", HttpStatus.UNAUTHORIZED));
        if (!Boolean.TRUE.equals(u.getAtivo())) {
            throw new NegocioException("Usuário inativo. Fale com o administrador.", HttpStatus.UNAUTHORIZED);
        }
        return LoginResponse.de(jwt.gerar(u), u, lojas.findById(u.getLojaId()).orElseThrow());
    }

    /** Dados atuais do usuário logado (a interface usa para manter nome e perfis em dia). */
    @GetMapping("/me")
    public UsuarioResponse me(Authentication auth) {
        return usuarioService.perfil(auth.getName());
    }

    @PutMapping("/me")
    public UsuarioResponse atualizarMe(Authentication auth, @Valid @RequestBody PerfilRequest req) {
        return usuarioService.atualizarPerfil(auth.getName(), req);
    }

    @PostMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarSenha(Authentication auth, @Valid @RequestBody AlterarSenhaRequest req) {
        usuarioService.alterarSenha(auth.getName(), req);
    }
}
