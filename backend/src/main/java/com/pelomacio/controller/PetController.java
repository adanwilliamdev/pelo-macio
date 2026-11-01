package com.pelomacio.controller;

import com.pelomacio.exception.RecursoNaoEncontradoException;
import com.pelomacio.model.Pet;
import com.pelomacio.repository.ClienteRepository;
import com.pelomacio.repository.PetRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pets")
public class PetController extends CrudController<Pet> {

    private final PetRepository pets;
    private final ClienteRepository clientes;

    public PetController(PetRepository pets, ClienteRepository clientes) {
        super(pets, "Pet");
        this.pets = pets;
        this.clientes = clientes;
    }

    @Override
    protected List<Pet> listar(Map<String, String> filtros) {
        String clienteId = filtros.get("clienteId");
        if (clienteId != null && !clienteId.isBlank()) {
            return pets.findByClienteId(Long.valueOf(clienteId), Sort.by("nome"));
        }
        return pets.findByNomeContainingIgnoreCase(filtros.getOrDefault("busca", ""), Sort.by("nome"));
    }

    /** O tutor chega como {"id": ...}: recarrega pelo repositório (filtrado por loja). */
    @Override
    protected void antesDeSalvar(Pet pet) {
        Long clienteId = pet.getCliente().getId();
        pet.setCliente(clientes.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", clienteId)));
    }
}
