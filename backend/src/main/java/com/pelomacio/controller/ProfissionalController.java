package com.pelomacio.controller;

import com.pelomacio.model.Profissional;
import com.pelomacio.repository.ProfissionalRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profissionais")
public class ProfissionalController extends CrudController<Profissional> {

    private final ProfissionalRepository profissionais;

    public ProfissionalController(ProfissionalRepository profissionais) {
        super(profissionais, "Profissional");
        this.profissionais = profissionais;
    }

    @Override
    protected List<Profissional> listar(Map<String, String> filtros) {
        return profissionais.findAll(Sort.by("nome"));
    }

    @Override
    protected void aoAtualizar(Profissional atual, Profissional novo) {
        novo.setUsuario(atual.getUsuario());
    }
}
