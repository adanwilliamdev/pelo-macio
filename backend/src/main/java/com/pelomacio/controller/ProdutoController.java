package com.pelomacio.controller;

import com.pelomacio.dto.EntradaEstoqueRequest;
import com.pelomacio.model.Produto;
import com.pelomacio.repository.ProdutoRepository;
import com.pelomacio.service.EstoqueService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController extends CrudController<Produto> {

    private final ProdutoRepository produtos;
    private final EstoqueService estoque;

    public ProdutoController(ProdutoRepository produtos, EstoqueService estoque) {
        super(produtos, "Produto");
        this.produtos = produtos;
        this.estoque = estoque;
    }

    @Override
    protected List<Produto> listar(Map<String, String> filtros) {
        return produtos.findByNomeContainingIgnoreCase(filtros.getOrDefault("busca", ""), Sort.by("nome"));
    }

    @PostMapping("/{id}/entrada")
    public Produto entrada(@PathVariable Long id, @Valid @RequestBody EntradaEstoqueRequest req) {
        return estoque.entrada(id, req.quantidade(), req.custoTotal());
    }
}
