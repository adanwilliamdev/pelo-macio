package com.pelomacio.integracao;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

/**
 * O que importa neste produto: uma loja nunca enxerga nem altera dados de outra, e o banco impede
 * atendimentos sobrepostos. Estes testes usam o app inteiro (filtros, Hibernate, Flyway) contra um PostgreSQL real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIf("com.pelomacio.integracao.Banco#disponivel")
class MultiLojaIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    record Conta(String token, String slug, String email) {
        String bearer() { return "Bearer " + token; }
    }

    private Conta cadastrar(String nome) throws Exception {
        String email = "admin-" + UUID.randomUUID() + "@teste.com";
        String corpo = json.writeValueAsString(Map.of("nomeLoja", nome + " " + UUID.randomUUID().toString().substring(0, 6),
                "nome", "Admin " + nome, "email", email, "senha", "senha12345"));
        String resp = mvc.perform(post("/api/publico/cadastro").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode n = json.readTree(resp);
        return new Conta(n.get("token").asText(), n.get("lojaSlug").asText(), email);
    }

    private long criar(Conta c, String url, String corpo) throws Exception {
        String resp = mvc.perform(post(url).header("Authorization", c.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("id").asLong();
    }

    private static LocalDate proximoDiaUtil() {
        LocalDate d = LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(3);
        while (d.getDayOfWeek() == DayOfWeek.SUNDAY) d = d.plusDays(1);
        return d;
    }

    // ───────────── isolamento ─────────────

    @Test
    void umaLojaNaoEnxergaNemAlteraDadosDeOutra() throws Exception {
        Conta a = cadastrar("Loja A");
        Conta b = cadastrar("Loja B");

        long clienteA = criar(a, "/api/clientes", "{\"nome\":\"Tutor A\",\"telefone\":\"11911112222\"}");
        long petA = criar(a, "/api/pets",
                "{\"cliente\":{\"id\":" + clienteA + "},\"nome\":\"Rex\",\"especie\":\"CACHORRO\",\"porte\":\"MEDIO\"}");
        long produtoA = criar(a, "/api/produtos", "{\"nome\":\"Shampoo A\",\"quantidadeEstoque\":5,\"custoUnitario\":10,\"estoqueMinimo\":1}");

        // leitura
        mvc.perform(get("/api/clientes").header("Authorization", b.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id==" + clienteA + ")]").isEmpty());
        mvc.perform(get("/api/clientes/" + clienteA).header("Authorization", b.bearer())).andExpect(status().isNotFound());
        mvc.perform(get("/api/pets/" + petA).header("Authorization", b.bearer())).andExpect(status().isNotFound());
        mvc.perform(get("/api/pets").header("Authorization", b.bearer()).param("clienteId", String.valueOf(clienteA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));

        // escrita e exclusão
        mvc.perform(put("/api/clientes/" + clienteA).header("Authorization", b.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Invadido\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/clientes/" + clienteA).header("Authorization", b.bearer())).andExpect(status().isNotFound());

        // não dá para apontar para registros de outra loja
        mvc.perform(post("/api/pets").header("Authorization", b.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cliente\":{\"id\":" + clienteA + "},\"nome\":\"X\",\"especie\":\"GATO\",\"porte\":\"PEQUENO\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/servicos").header("Authorization", b.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"S\",\"duracaoMinutos\":30,\"preco\":10,\"insumos\":[{\"produto\":{\"id\":" + produtoA + "},\"quantidade\":1}]}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/agendamentos").header("Authorization", b.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"petId\":" + petA + ",\"profissionalId\":1,\"servicoId\":1,\"inicio\":\"2030-01-07T10:00:00\"}"))
                .andExpect(status().isNotFound());

        // e nada disso mexeu no que é da loja A
        mvc.perform(get("/api/clientes/" + clienteA).header("Authorization", a.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Tutor A"));
    }

    @Test
    void usuariosTambemSaoIsoladosPorLoja() throws Exception {
        Conta a = cadastrar("Loja A");
        Conta b = cadastrar("Loja B");

        mvc.perform(get("/api/usuarios").header("Authorization", a.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value(a.email()));
        mvc.perform(get("/api/usuarios").header("Authorization", b.bearer()))
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].email").value(b.email()));
    }

    @Test
    void semTokenNaoAcessaAApiInterna() throws Exception {
        mvc.perform(get("/api/clientes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/loja")).andExpect(status().isUnauthorized());
    }

    @Test
    void cadastroNovoJaNasceUtilizavelEEmailNaoSeRepete() throws Exception {
        Conta a = cadastrar("Loja A");

        mvc.perform(get("/api/dashboard").header("Authorization", a.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.receitaSerie", hasSize(6)));
        mvc.perform(get("/api/servicos").header("Authorization", a.bearer()))
                .andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(post("/api/publico/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nomeLoja", "Outra", "nome", "X", "email", a.email(), "senha", "senha12345"))))
                .andExpect(status().isConflict());
    }

    // ───────────── agendamento público ─────────────

    @Test
    void tutorAgendaSemLoginEAReservaApareceSoNaLojaCerta() throws Exception {
        Conta a = cadastrar("Loja A");
        Conta b = cadastrar("Loja B");
        LocalDate dia = proximoDiaUtil();

        String base = "/api/publico/lojas/" + a.slug();
        mvc.perform(get(base)).andExpect(status().isOk()).andExpect(jsonPath("$.slug").value(a.slug()));
        JsonNode servicos = json.readTree(mvc.perform(get(base + "/servicos")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3))).andReturn().getResponse().getContentAsString());
        long servicoId = servicos.get(0).get("id").asLong();

        mvc.perform(get(base + "/horarios").param("servicoId", String.valueOf(servicoId))
                        .param("porte", "PEQUENO").param("data", dia.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].inicio").value(dia + "T09:00:00"));

        String reserva = json.writeValueAsString(Map.of("nome", "Ana", "telefone", "(11) 98888-7777", "petNome", "Thor",
                "especie", "CACHORRO", "porte", "PEQUENO", "servicoId", servicoId, "inicio", dia + "T10:00:00"));
        mvc.perform(post(base + "/agendamentos").contentType(MediaType.APPLICATION_JSON).content(reserva))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.pet").value("Thor"))
                .andExpect(jsonPath("$.telefone").doesNotExist());

        // o horário já foi ocupado (a loja nova só tem 1 profissional)
        mvc.perform(post(base + "/agendamentos").contentType(MediaType.APPLICATION_JSON).content(reserva))
                .andExpect(status().isConflict());
        mvc.perform(get(base + "/horarios").param("servicoId", String.valueOf(servicoId))
                        .param("porte", "PEQUENO").param("data", dia.toString()))
                .andExpect(jsonPath("$[?(@.inicio=='" + dia + "T10:00:00')]").isEmpty());

        // aparece na agenda da loja A, marcada como online, e não na da B
        mvc.perform(get("/api/agendamentos").header("Authorization", a.bearer()).param("de", dia.toString()).param("ate", dia.toString()))
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].origem").value("ONLINE"))
                .andExpect(jsonPath("$[0].pet.nome").value("Thor"));
        mvc.perform(get("/api/agendamentos").header("Authorization", b.bearer()).param("de", dia.toString()).param("ate", dia.toString()))
                .andExpect(jsonPath("$", hasSize(0)));

        // serviço da loja A não pode ser usado na página da loja B
        mvc.perform(post("/api/publico/lojas/" + b.slug() + "/agendamentos").contentType(MediaType.APPLICATION_JSON).content(reserva))
                .andExpect(status().isNotFound());
    }

    @Test
    void paginaPublicaIgnoraOTokenDeOutraLoja() throws Exception {
        Conta a = cadastrar("Loja A");
        Conta b = cadastrar("Loja B");

        String idsDeB = mvc.perform(get("/api/publico/lojas/" + b.slug() + "/servicos"))
                .andReturn().getResponse().getContentAsString();
        // um administrador da loja A (com token) abrindo a página pública da loja B continua vendo a loja B
        String comTokenDeA = mvc.perform(get("/api/publico/lojas/" + b.slug() + "/servicos").header("Authorization", a.bearer()))
                .andReturn().getResponse().getContentAsString();

        assertEquals(idsDeB, comTokenDeA);
    }

    @Test
    void slugDesconhecidoOuAgendamentoOnlineDesligadoRetorna404() throws Exception {
        Conta a = cadastrar("Loja A");
        mvc.perform(get("/api/publico/lojas/nao-existe-" + UUID.randomUUID().toString().substring(0, 6))).andExpect(status().isNotFound());

        jdbc.update("update lojas set agendamento_online = false where slug = ?", a.slug());
        mvc.perform(get("/api/publico/lojas/" + a.slug())).andExpect(status().isNotFound());
    }

    // ───────────── garantias do banco ─────────────

    private long inserirLoja() {
        String slug = "bd-" + UUID.randomUUID().toString().substring(0, 8);
        return jdbc.queryForObject("insert into lojas (nome, slug) values (?, ?) returning id", Long.class, slug, slug);
    }

    private long inserir(String sql, Object... args) {
        return jdbc.queryForObject(sql + " returning id", Long.class, args);
    }

    @Test
    void bancoRecusaVinculoEntreLojasDiferentes() {
        long loja1 = inserirLoja();
        long loja2 = inserirLoja();
        long clienteDaLoja1 = inserir("insert into clientes (loja_id, nome) values (?, 'Tutor')", loja1);

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "insert into pets (loja_id, cliente_id, nome, especie, porte) values (?, ?, 'Rex', 'CACHORRO', 'MEDIO')",
                loja2, clienteDaLoja1));
        assertDoesNotThrow(() -> jdbc.update(
                "insert into pets (loja_id, cliente_id, nome, especie, porte) values (?, ?, 'Rex', 'CACHORRO', 'MEDIO')",
                loja1, clienteDaLoja1));
    }

    @Test
    void bancoRecusaAtendimentosSobrepostosDoMesmoProfissional() {
        long loja = inserirLoja();
        long cliente = inserir("insert into clientes (loja_id, nome) values (?, 'Tutor')", loja);
        long pet = inserir("insert into pets (loja_id, cliente_id, nome, especie, porte) values (?, ?, 'Rex', 'CACHORRO', 'MEDIO')", loja, cliente);
        long prof = inserir("insert into profissionais (loja_id, nome) values (?, 'Camila')", loja);
        long outro = inserir("insert into profissionais (loja_id, nome) values (?, 'Diego')", loja);
        long servico = inserir("insert into servicos (loja_id, nome, duracao_minutos, preco) values (?, 'Banho', 60, 50)", loja);

        String sql = "insert into agendamentos (loja_id, cliente_id, pet_id, profissional_id, servico_id, "
                + "data_hora_inicio, data_hora_fim, status, encaixe) values (?, ?, ?, ?, ?, ?::timestamp, ?::timestamp, ?, ?)";
        jdbc.update(sql, loja, cliente, pet, prof, servico, "2030-01-07 10:00", "2030-01-07 11:00", "AGENDADO", false);

        // mesmo profissional, horário sobreposto: recusado
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(sql, loja, cliente, pet, prof, servico,
                "2030-01-07 10:30", "2030-01-07 11:30", "AGENDADO", false));
        // colado (termina quando o outro começa): permitido
        assertDoesNotThrow(() -> jdbc.update(sql, loja, cliente, pet, prof, servico,
                "2030-01-07 11:00", "2030-01-07 12:00", "AGENDADO", false));
        // outro profissional no mesmo horário: permitido
        assertDoesNotThrow(() -> jdbc.update(sql, loja, cliente, pet, outro, servico,
                "2030-01-07 10:00", "2030-01-07 11:00", "AGENDADO", false));
        // encaixe e cancelado não contam
        assertDoesNotThrow(() -> jdbc.update(sql, loja, cliente, pet, prof, servico,
                "2030-01-07 10:00", "2030-01-07 11:00", "AGENDADO", true));
        assertDoesNotThrow(() -> jdbc.update(sql, loja, cliente, pet, prof, servico,
                "2030-01-07 10:00", "2030-01-07 11:00", "CANCELADO", false));
    }
}
