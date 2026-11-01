package com.pelomacio.controller;

import com.pelomacio.exception.RecursoNaoEncontradoException;
import com.pelomacio.model.Identificavel;
import jakarta.validation.Valid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/** CRUD genérico: subclasses só definem o mapeamento e, se preciso, filtros e regras de atualização. */
public abstract class CrudController<T extends Identificavel> {

    protected final JpaRepository<T, Long> repo;
    private final String nomeRecurso;

    protected CrudController(JpaRepository<T, Long> repo, String nomeRecurso) {
        this.repo = repo;
        this.nomeRecurso = nomeRecurso;
    }

    protected List<T> listar(Map<String, String> filtros) {
        return repo.findAll();
    }

    /** Gancho para preservar campos que não vêm do JSON (ex.: vínculo com usuário). */
    protected void aoAtualizar(T atual, T novo) {}

    /**
     * Gancho chamado antes de gravar (criação e atualização). Relações que chegam como {"id": ...} no JSON devem ser
     * trocadas aqui pela entidade carregada do banco: assim o filtro de loja se aplica e não dá para apontar para
     * um registro de outra loja.
     */
    protected void antesDeSalvar(T body) {}

    @GetMapping
    public List<T> list(@RequestParam Map<String, String> filtros) {
        return listar(filtros);
    }

    @GetMapping("/{id}")
    public T get(@PathVariable Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException(nomeRecurso, id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public T create(@Valid @RequestBody T body) {
        body.setId(null);
        body.setLojaId(null); // a loja é sempre a da sessão (o Hibernate preenche)
        antesDeSalvar(body);
        return repo.save(body);
    }

    @PutMapping("/{id}")
    @Transactional
    public T update(@PathVariable Long id, @Valid @RequestBody T body) {
        T atual = get(id);
        body.setId(id);
        body.setLojaId(atual.getLojaId());
        aoAtualizar(atual, body);
        antesDeSalvar(body);
        return repo.save(body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        // deleteById faria um find() por baixo; excluir a entidade já buscada (e filtrada por loja) é mais seguro
        repo.delete(get(id));
    }
}
