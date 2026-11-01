package com.pelomacio.config;

import com.pelomacio.model.*;
import com.pelomacio.model.Agendamento.Status;
import com.pelomacio.model.Pet.Especie;
import com.pelomacio.model.Pet.Porte;
import com.pelomacio.repository.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Conteúdo de uma loja de exemplo: equipe, serviços, estoque, tutores, pets, agenda e caixa. */
@Component
@ConditionalOnProperty(name = "pelomacio.demo.enabled", havingValue = "true")
public class DemoLoja {
    private final ProfissionalRepository profissionais;
    private final ServicoRepository servicos;
    private final ProdutoRepository produtos;
    private final ClienteRepository clientes;
    private final PetRepository pets;
    private final AgendamentoRepository agendamentos;
    private final LancamentoRepository lancamentos;

    public DemoLoja(ProfissionalRepository profissionais, ServicoRepository servicos, ProdutoRepository produtos,
             ClienteRepository clientes, PetRepository pets, AgendamentoRepository agendamentos,
             LancamentoRepository lancamentos) {
        this.profissionais = profissionais;
        this.servicos = servicos;
        this.produtos = produtos;
        this.clientes = clientes;
        this.pets = pets;
        this.agendamentos = agendamentos;
        this.lancamentos = lancamentos;
    }

    /** Deve ser chamado dentro de uma transação aberta com a loja já definida no TenantContext. */
    public void popular(LocalDateTime agora, int variante) {
        LocalDate hoje = agora.toLocalDate();
        List<Profissional> equipe = profissionais.saveAll(List.of(
                prof(variante == 0 ? "Camila Rocha" : "Bruna Teixeira", "Banho e tosa", "40"),
                prof(variante == 0 ? "Diego Lima" : "Rafael Costa", "Tosa avançada", "35"),
                prof(variante == 0 ? "Renata Alves" : "Marina Souza", "Banho e hidratação", "40")));

        Produto shampoo = produtos.save(produto("Shampoo neutro", "Higiene", "1.2", "L", "28.00", "2"));
        Produto condicionador = produtos.save(produto("Condicionador", "Higiene", "6", "L", "32.00", "2"));
        produtos.save(produto("Perfume pet", "Finalização", "0.4", "L", "45.00", "0.5"));
        produtos.save(produto("Lenço higiênico", "Higiene", "300", "un", "0.15", "100"));

        Servico banho = servicos.save(servico("Banho", "Banho", 60, "55", 75, "75", 90, "95", shampoo, "0.08"));
        Servico tosaHigienica = servicos.save(servico("Tosa higiênica", "Tosa", 30, "35", 40, "45", 50, "60", null, null));
        Servico banhoETosa = servicos.save(servico("Banho e tosa", "Banho", 90, "85", 110, "115", 130, "145", shampoo, "0.1"));
        servicos.save(servico("Hidratação", "Spa", 45, "40", 60, "55", 75, "70", condicionador, "0.05"));

        List<Pet> todos = new ArrayList<>();
        String[][] tutores = {
                {"Ana Beatriz Lopes", "11987654321"}, {"Carlos Mendes", "11976543210"},
                {"Fernanda Dias", "11965432109"}, {"Rafael Lima", "11954321098"},
                {"Juliana Prado", "11943210987"}, {"Marcos Vieira", "11932109876"}};
        Object[][] bichos = {
                {0, "Thor", Especie.CACHORRO, "Golden Retriever", Porte.GRANDE},
                {0, "Mel", Especie.CACHORRO, "Shih-tzu", Porte.PEQUENO},
                {1, "Luna", Especie.GATO, "Siamês", Porte.PEQUENO},
                {2, "Bob", Especie.CACHORRO, "Labrador", Porte.GRANDE},
                {3, "Nina", Especie.CACHORRO, "Poodle", Porte.PEQUENO},
                {3, "Pipoca", Especie.CACHORRO, "SRD", Porte.MEDIO},
                {4, "Simba", Especie.GATO, "Persa", Porte.PEQUENO},
                {5, "Rex", Especie.CACHORRO, "Border Collie", Porte.MEDIO}};
        List<Cliente> clientesSalvos = new ArrayList<>();
        for (String[] t : tutores) {
            clientesSalvos.add(clientes.save(Cliente.builder().nome(t[0]).telefone(t[1]).build()));
        }
        for (Object[] b : bichos) {
            todos.add(pets.save(Pet.builder().cliente(clientesSalvos.get((Integer) b[0])).nome((String) b[1])
                    .especie((Especie) b[2]).raca((String) b[3]).porte((Porte) b[4])
                    .observacoes(b[1].equals("Thor") ? "Agitado com secador; usar o mais baixo." : null).build()));
        }

        // agenda: alguns atendimentos hoje (profissionais diferentes, então sem conflito) e nos próximos dias
        // {hora, minuto, índice do profissional, índice do serviço, índice do pet}
        int[][] doDia = {{9, 0, 0, 0, 0}, {10, 30, 1, 1, 2}, {14, 0, 2, 2, 4}, {16, 0, 0, 1, 6}};
        Servico[] cardapio = {banhoETosa, banho, tosaHigienica};
        for (int i = 0; i < doDia.length; i++) {
            LocalDateTime inicio = hoje.atTime(doDia[i][0], doDia[i][1]);
            Status st = inicio.isBefore(agora) ? Status.CONCLUIDO : (i % 2 == 0 ? Status.CONFIRMADO : Status.AGENDADO);
            Agendamento.Origem origem = i == 3 ? Agendamento.Origem.ONLINE : Agendamento.Origem.INTERNA;
            marcar(todos.get(doDia[i][4]), equipe.get(doDia[i][2]), cardapio[doDia[i][3]], inicio, st, origem);
        }
        marcar(todos.get(5), equipe.get(0), banho, hoje.plusDays(1).atTime(9, 0), Status.AGENDADO, Agendamento.Origem.ONLINE);
        marcar(todos.get(6), equipe.get(1), tosaHigienica, hoje.plusDays(1).atTime(11, 0), Status.CONFIRMADO, Agendamento.Origem.INTERNA);
        marcar(todos.get(7), equipe.get(2), banhoETosa, hoje.plusDays(2).atTime(15, 0), Status.AGENDADO, Agendamento.Origem.INTERNA);

        // caixa dos últimos 6 meses (com tendência de alta), para o painel ter o que mostrar
        Random r = new Random(42 + variante);
        for (int m = 5; m >= 0; m--) {
            LocalDate base = hoje.withDayOfMonth(1).minusMonths(m);
            int atendimentos = 22 + (5 - m) * 3 + r.nextInt(5);
            BigDecimal total = BigDecimal.ZERO;
            for (int i = 0; i < atendimentos; i += 6) {
                BigDecimal v = BigDecimal.valueOf(300 + r.nextInt(220));
                total = total.add(v);
                lancamentos.save(Lancamento.builder().tipo(Lancamento.Tipo.RECEITA).categoria("SERVICO")
                        .descricao("Atendimentos da semana").valor(v)
                        .data(base.plusDays(Math.min(i, base.lengthOfMonth() - 1))).build());
            }
            lancamentos.save(Lancamento.builder().tipo(Lancamento.Tipo.DESPESA).categoria("OUTROS")
                    .descricao("Aluguel e contas").valor(BigDecimal.valueOf(900)).data(base).build());
        }
    }

    private void marcar(Pet pet, Profissional prof, Servico servico, LocalDateTime inicio, Status status, Agendamento.Origem origem) {
        agendamentos.save(Agendamento.builder().cliente(pet.getCliente()).pet(pet).profissional(prof).servico(servico)
                .dataHoraInicio(inicio).dataHoraFim(inicio.plusMinutes(servico.duracaoPara(pet.getPorte())))
                .status(status).origem(origem).build());
    }

    private static Profissional prof(String nome, String especialidade, String comissao) {
        return Profissional.builder().nome(nome).especialidade(especialidade).comissaoPadrao(new BigDecimal(comissao)).build();
    }

    private static Produto produto(String nome, String tipo, String qtd, String un, String custo, String minimo) {
        return Produto.builder().nome(nome).tipo(tipo).quantidadeEstoque(new BigDecimal(qtd)).unidade(un)
                .custoUnitario(new BigDecimal(custo)).estoqueMinimo(new BigDecimal(minimo)).build();
    }

    private static Servico servico(String nome, String cat, int dp, String pp, int dm, String pm, int dg, String pg,
                                   Produto insumo, String qtdInsumo) {
        Servico s = Servico.builder().nome(nome).categoria(cat).duracaoMinutos(dp).preco(new BigDecimal(pp))
                .duracaoMedio(dm).precoMedio(new BigDecimal(pm)).duracaoGrande(dg).precoGrande(new BigDecimal(pg)).build();
        if (insumo != null) {
            s.getInsumos().add(ServicoInsumo.builder().produto(insumo).quantidade(new BigDecimal(qtdInsumo)).build());
        }
        return s;
    }
}
