package com.pelomacio.controller;

import com.pelomacio.dto.ComissaoResumo;
import com.pelomacio.dto.ResumoFinanceiro;
import com.pelomacio.model.Lancamento;
import com.pelomacio.exception.RecursoNaoEncontradoException;
import com.pelomacio.repository.LancamentoRepository;
import com.pelomacio.repository.ProfissionalRepository;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Acesso restrito à ADMINISTRADOR (ver SecurityConfig). */
@RestController
@RequestMapping("/api/financeiro")
public class FinanceiroController {

    private final LancamentoRepository lancamentos;

    private final ProfissionalRepository profissionais;

    public FinanceiroController(LancamentoRepository lancamentos, ProfissionalRepository profissionais) {
        this.lancamentos = lancamentos;
        this.profissionais = profissionais;
    }

    @GetMapping("/lancamentos")
    public List<Lancamento> listar(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return lancamentos.findByDataBetweenOrderByDataDescIdDesc(de, ate);
    }

    @PostMapping("/lancamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public Lancamento criar(@Valid @RequestBody Lancamento lancamento) {
        lancamento.setId(null);
        lancamento.setLojaId(null);
        if (lancamento.getProfissional() != null) {
            Long id = lancamento.getProfissional().getId();
            lancamento.setProfissional(profissionais.findById(id)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional", id)));
        }
        return lancamentos.save(lancamento);
    }

    @DeleteMapping("/lancamentos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        lancamentos.delete(lancamentos.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lançamento", id)));
    }

    @GetMapping("/resumo")
    public ResumoFinanceiro resumo(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        BigDecimal receitas = lancamentos.somar(Lancamento.Tipo.RECEITA, de, ate);
        BigDecimal despesas = lancamentos.somar(Lancamento.Tipo.DESPESA, de, ate);
        return new ResumoFinanceiro(receitas, despesas, receitas.subtract(despesas));
    }

    @GetMapping("/comissoes")
    public List<ComissaoResumo> comissoes(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return lancamentos.comissoesPorProfissional(de, ate);
    }
}
