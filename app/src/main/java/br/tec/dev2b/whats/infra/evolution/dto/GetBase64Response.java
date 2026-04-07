package br.tec.dev2b.whats.infra.evolution.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Resposta de POST /chat/getBase64FromMediaMessage/{instance}.
 *
 * <pre>
 * {
 *   "base64":   "data:application/pdf;base64,JVBERi0...", // ou sem prefixo data URI
 *   "mimetype": "application/pdf"
 * }
 * </pre>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetBase64Response {

    /** Conteúdo base64, com ou sem prefixo {@code data:<mime>;base64,}. */
    private String base64;

    /** MIME type reportado pela Evolution API (pode ser null). */
    private String mimetype;
}
