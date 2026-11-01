package com.pelomacio.controller;

import com.pelomacio.dto.*;
import com.pelomacio.model.Pet;
import com.pelomacio.service.AgendamentoPublicoService;
import com.pelomacio.service.LojaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Rotas sem login. As de {@code /lojas/{slug}/...} passam pelo PublicTenantFilter, que resolve o slug e define a
 * loja; o {@code slug} aparece aqui só para compor a rota.
 */
@RestController
@RequestMapping("/api/publico")
public class PublicoController {

    private final AgendamentoPublicoService agendamento;
    private final LojaService lojaService;

    public PublicoController(AgendamentoPublicoService agendamento, LojaService lojaService) {
        this.agendamento = agendamento;
        this.lojaService = lojaService;
    }

    @GetMapping("/lojas/{slug}")
    public LojaPublicaResponse loja(@PathVariable String slug) {
        return agendamento.loja();
    }

    @GetMapping("/lojas/{slug}/servicos")
    public List<ServicoPublicoResponse> servicos(@PathVariable String slug) {
        return agendamento.servicos();
    }

    @GetMapping("/lojas/{slug}/profissionais")
    public List<ProfissionalPublicoResponse> profissionais(@PathVariable String slug) {
        return agendamento.profissionais();
    }

    @GetMapping("/lojas/{slug}/horarios")
    public List<HorarioLivre> horarios(@PathVariable String slug,
                                       @RequestParam Long servicoId,
                                       @RequestParam Pet.Porte porte,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                                       @RequestParam(required = false) Long profissionalId) {
        return agendamento.horarios(servicoId, porte, data, profissionalId);
    }

    @PostMapping("/lojas/{slug}/agendamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaPublicaResponse reservar(@PathVariable String slug, @Valid @RequestBody ReservaPublicaRequest req) {
        return agendamento.reservar(req);
    }

    /** Cria uma loja nova com o administrador e devolve o login pronto. */
    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse cadastro(@Valid @RequestBody CadastroLojaRequest req) {
        return lojaService.cadastrar(req);
    }
}
