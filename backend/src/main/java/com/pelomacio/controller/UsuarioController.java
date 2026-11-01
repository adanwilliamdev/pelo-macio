package com.pelomacio.controller;

import com.pelomacio.dto.*;
import com.pelomacio.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Gestão de usuários e perfis. Restrito à ADMINISTRADOR em SecurityConfig. */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return service.listar();
    }

    @GetMapping("/opcoes-vinculo")
    public OpcoesVinculo opcoesVinculo() {
        return service.opcoesVinculo();
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody UsuarioCriarRequest req) {
        return service.criar(req);
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioAtualizarRequest req,
                                     Authentication auth) {
        return service.atualizar(id, req, auth.getName());
    }

    @PatchMapping("/{id}/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redefinirSenha(@PathVariable Long id, @Valid @RequestBody SenhaRequest req) {
        service.redefinirSenha(id, req.novaSenha());
    }
}
