package com.pelomacio.dto;

import com.pelomacio.model.Pet;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record ReservaPublicaRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Size(max = 30) String telefone,
        @Email @Size(max = 160) String email,
        @NotBlank @Size(max = 80) String petNome,
        @NotNull Pet.Especie especie,
        @Size(max = 80) String raca,
        @NotNull Pet.Porte porte,
        @NotNull Long servicoId,
        /** Vazio = "qualquer profissional". */
        Long profissionalId,
        @NotNull LocalDateTime inicio) {}
