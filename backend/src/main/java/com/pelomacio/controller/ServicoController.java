package com.pelomacio.controller;

import com.pelomacio.exception.RecursoNaoEncontradoException;
import com.pelomacio.model.Servico;
import com.pelomacio.model.ServicoInsumo;
import com.pelomacio.repository.ProdutoRepository;
import com.pelomacio.repository.ServicoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/servicos")
public class ServicoController extends CrudController<Servico> {

    private final ServicoRepository servicos;

    private final ProdutoRepository produtos;

    public ServicoController(ServicoRepository servicos, ProdutoRepository produtos) {
        super(servicos, "Serviço");
        this.servicos = servicos;
        this.produtos = produtos;
    }

    /** Cada insumo aponta para um produto: recarrega pelo repositório para garantir que é da mesma loja. */
    @Override
    protected void antesDeSalvar(Servico servico) {
        if (servico.getInsumos() == null) return;
        for (ServicoInsumo insumo : servico.getInsumos()) {
            Long produtoId = insumo.getProduto().getId();
            insumo.setId(null);
            insumo.setProduto(produtos.findById(produtoId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", produtoId)));
        }
    }

    @Override
    protected List<Servico> listar(Map<String, String> filtros) {
        return servicos.findByNomeContainingIgnoreCase(filtros.getOrDefault("busca", ""), Sort.by("nome"));
    }
}
