package com.pelomacio.integration.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.Map;

@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    private final String token;
    private final String phoneNumberId;
    private final RestClient http;

    public WhatsAppService(@Value("${pelomacio.whatsapp.api-url}") String apiUrl,
                           @Value("${pelomacio.whatsapp.token}") String token,
                           @Value("${pelomacio.whatsapp.phone-number-id}") String phoneNumberId) {
        this.token = token;
        this.phoneNumberId = phoneNumberId;
        this.http = RestClient.builder().baseUrl(apiUrl).build();
    }

    public boolean configurado() {
        return !token.startsWith("SEU_") && !phoneNumberId.startsWith("SEU_");
    }

    /** Retorna true se a mensagem foi aceita pela API (ou false se não configurado/erro). */
    public boolean enviarMensagem(String telefone, String mensagem) {
        String numero = normalizar(telefone);
        if (numero == null) return false;
        if (!configurado()) {
            log.info("[WhatsApp - modo simulado] Para: {} | Msg: {}", numero, mensagem);
            return false;
        }
        try {
            http.post().uri("/{id}/messages", phoneNumberId)
                    .header("Authorization", "Bearer " + token)
                    .body(Map.of("messaging_product", "whatsapp", "to", numero,
                            "type", "text", "text", Map.of("body", mensagem)))
                    .retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.error("Falha ao enviar WhatsApp para {}: {}", numero, e.getMessage());
            return false;
        }
    }

    /** Mantém só dígitos e assume DDI 55 para números brasileiros de 10 ou 11 dígitos. */
    static String normalizar(String telefone) {
        if (telefone == null) return null;
        String d = telefone.replaceAll("\\D", "");
        if (d.length() == 10 || d.length() == 11) d = "55" + d;
        return d.length() >= 12 ? d : null;
    }
}
