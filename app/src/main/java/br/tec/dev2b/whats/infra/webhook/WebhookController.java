package br.tec.dev2b.whats.infra.webhook;

import br.tec.dev2b.whats.infra.evolution.dto.WebhookPayload;
import br.tec.dev2b.whats.instancia.service.InstanciaService;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Recebe eventos da Evolution API via HTTP POST e roteia para os handlers.
 *
 * Eventos tratados:
 *   - connection.update -> atualiza status da instancia
 *   - qrcode.updated    -> atualiza QR Code
 *   - messages.upsert   -> delega ao MensagemUpsertService
 */
@RestController
@RequestMapping("/api/v1/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final InstanciaService instanciaService;
    private final MensagemUpsertService mensagemUpsertService;
    private final ObjectMapper objectMapper;

    @PostMapping("/evolution")
    public ResponseEntity<Void> receberEvento(@RequestBody WebhookPayload payload) {
        if (payload == null || payload.getEvent() == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            log.info("[WEBHOOK] event={} instance={}\n{}",
                    payload.getEvent(), payload.getInstance(),
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload));
        } catch (Exception e) {
            log.info("[WEBHOOK] event={} instance={} (erro ao serializar payload)",
                    payload.getEvent(), payload.getInstance());
        }

        switch (payload.getEvent()) {
            case "connection.update" -> handleConnectionUpdate(payload);
            case "qrcode.updated"    -> handleQrCodeUpdated(payload);
            case "messages.upsert"   -> mensagemUpsertService.processar(payload);
            case "messages.update"   -> log.debug("messages.update ignorado por ora");
            default -> log.debug("Evento nao tratado: {}", payload.getEvent());
        }

        return ResponseEntity.ok().build();
    }

    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void handleConnectionUpdate(WebhookPayload payload) {
        Map<String, Object> data = payload.getData();
        if (data == null) return;

        String state  = (String) data.get("state");
        String numero = null;

        Object instanceObj = data.get("instance");
        if (instanceObj instanceof Map<?, ?> instanceMap) {
            Object owner = instanceMap.get("owner");
            if (owner instanceof String ownerStr) {
                numero = ownerStr.replace("@s.whatsapp.net", "");
            }
        }

        String status = switch (state != null ? state.toLowerCase() : "") {
            case "open"       -> "CONECTADA";
            case "connecting" -> "CONECTANDO";
            case "close"      -> "DESCONECTADA";
            default           -> "DESCONECTADA";
        };

        instanciaService.atualizarStatusPorInstanceName(payload.getInstance(), status, numero, null);
    }

    private void handleQrCodeUpdated(WebhookPayload payload) {
        Map<String, Object> data = payload.getData();
        if (data == null) return;

        String qrCodeBase64 = (String) data.get("base64");
        instanciaService.atualizarStatusPorInstanceName(
                payload.getInstance(), "CONECTANDO", null, qrCodeBase64);
    }
}
