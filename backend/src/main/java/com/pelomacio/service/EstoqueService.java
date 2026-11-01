package com.pelomacio.service;

import com.pelomacio.exception.RecursoNaoEncontradoException;
import com.pelomacio.model.Lancamento;
import com.pelomacio.model.Produto;
import com.pelomacio.repository.LancamentoRepository;
import com.pelomacio.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class EstoqueService {

    private final ProdutoRepository produtos;
    private final LancamentoRepository lancamentos;

    public EstoqueService(ProdutoRepository produtos, LancamentoRepository lancamentos) {
        this.produtos = produtos;
        this.lancamentos = lancamentos;
    }

    /** Entrada de estoque; se houver custo total, atualiza o custo unitário e lança a despesa. */
    @Transactional
    public Produto entrada(Long produtoId, BigDecimal quantidade, BigDecimal custoTotal) {
        Produto p = produtos.findById(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", produtoId));
        p.setQuantidadeEstoque(p.getQuantidadeEstoque().add(quantidade));
        if (custoTotal != null && custoTotal.signum() > 0) {
            p.setCustoUnitario(custoTotal.divide(quantidade, 2, RoundingMode.HALF_UP));
            lancamentos.save(Lancamento.builder()
                    .tipo(Lancamento.Tipo.DESPESA).categoria("COMPRA_ESTOQUE")
                    .descricao("Compra: " + p.getNome()).valor(custoTotal).data(LocalDate.now()).build());
        }
        return produtos.save(p);
    }
}
