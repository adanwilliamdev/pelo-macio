package com.pelomacio.controller;

import com.pelomacio.model.Cliente;
import com.pelomacio.repository.ClienteRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController extends CrudController<Cliente> {

    private final ClienteRepository clientes;

    public ClienteController(ClienteRepository clientes) {
        super(clientes, "Cliente");
        this.clientes = clientes;
    }

    @Override
    protected List<Cliente> listar(Map<String, String> filtros) {
        String busca = filtros.get("busca");
        Sort ordem = Sort.by("nome");
        if (busca == null || busca.isBlank()) return clientes.findAll(ordem);
        return clientes.findByNomeContainingIgnoreCaseOrTelefoneContaining(busca, busca, ordem);
    }

    @Override
    protected void aoAtualizar(Cliente atual, Cliente novo) {
        novo.setUsuario(atual.getUsuario());
    }
}
