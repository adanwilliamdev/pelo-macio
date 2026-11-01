package com.pelomacio.controller;

import com.pelomacio.dto.LojaAtualizarRequest;
import com.pelomacio.dto.LojaResponse;
import com.pelomacio.service.LojaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** Configuração da loja do usuário logado. A alteração é restrita à ADMINISTRADOR em SecurityConfig. */
@RestController
@RequestMapping("/api/loja")
public class LojaController {

    private final LojaService service;

    public LojaController(LojaService service) {
        this.service = service;
    }

    @GetMapping
    public LojaResponse atual() {
        return service.atual();
    }

    @PutMapping
    public LojaResponse atualizar(@Valid @RequestBody LojaAtualizarRequest req) {
        return service.atualizar(req);
    }
}
