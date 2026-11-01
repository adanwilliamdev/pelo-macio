package com.pelomacio.exception;

import com.pelomacio.dto.ErroResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErroResponse(400, "Dados inválidos", campos));
    }

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErroResponse> negocio(NegocioException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ErroResponse(ex.getStatus().value(), ex.getMessage(), null));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErroResponse(404, ex.getMessage(), null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> integridade(DataIntegrityViolationException ex) {
        // duas reservas simultâneas do mesmo horário: a exclusion constraint do banco recusa a segunda
        String causa = String.valueOf(ex.getMostSpecificCause().getMessage());
        if (causa.contains("ex_agendamento_sem_sobreposicao")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErroResponse(409, "Esse horário acabou de ser ocupado. Escolha outro.", null));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroResponse(409, "Registro vinculado a outros dados ou duplicado.", null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ErroResponse(400, "Corpo da requisição inválido.", null));
    }
}
