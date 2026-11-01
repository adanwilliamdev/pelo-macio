package com.pelomacio.controller;

import com.pelomacio.dto.AgendamentoRequest;
import com.pelomacio.dto.StatusRequest;
import com.pelomacio.model.Agendamento;
import com.pelomacio.service.AgendamentoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    private final AgendamentoService service;

    public AgendamentoController(AgendamentoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Agendamento> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) Long profissionalId) {
        LocalDate inicio = de != null ? de : LocalDate.now();
        return service.listar(inicio, ate != null ? ate : inicio, profissionalId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Agendamento criar(@Valid @RequestBody AgendamentoRequest req) {
        return service.criar(req);
    }

    @PutMapping("/{id}")
    public Agendamento atualizar(@PathVariable Long id, @Valid @RequestBody AgendamentoRequest req) {
        return service.atualizar(id, req);
    }

    @PatchMapping("/{id}/status")
    public Agendamento status(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        return service.mudarStatus(id, req.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }
}
