package br.tec.dev2b.whats.infra.webhook;

import br.tec.dev2b.whats.infra.evolution.dto.WebhookPayload;
import br.tec.dev2b.whats.instancia.service.InstanciaService;
import br.tec.dev2b.whats.mensagem.service.MensagemService;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Recebe eventos da Evolution API via HTTP POST.
 *
 * Eventos tratados:
 *   - connection.update → atualiza status da instância
 *   - qrcode.updated    → atualiza QR Code
 *   - messages.upsert   → salva mensagem recebida
 *   - messages.update   → atualiza status de mensagem enviada
 */
@RestController
@RequestMapping("/api/v1/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final InstanciaService instanciaService;
    private final MensagemService mensagemService;
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
            case "messages.upsert"   -> handleMessagesUpsert(payload);
            case "messages.update"   -> log.debug("messages.update ignorado por ora");
            default -> log.debug("Evento não tratado: {}", payload.getEvent());
        }

        return ResponseEntity.ok().build();
    }

    // ------------------------------------------------

    @SuppressWarnings("unchecked")
    private void handleConnectionUpdate(WebhookPayload payload) {
        Map<String, Object> data = payload.getData();
        if (data == null) return;

        String state = (String) data.get("state");
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
        instanciaService.atualizarStatusPorInstanceName(payload.getInstance(), "CONECTANDO", null, qrCodeBase64);
    }

    @SuppressWarnings("unchecked")
    private void handleMessagesUpsert(WebhookPayload payload) {
        Map<String, Object> data = payload.getData();
        if (data == null) return;

        Object messagesObj = data.get("messages");
        if (!(messagesObj instanceof List<?> messagesList)) return;

        for (Object msgObj : messagesList) {
            if (!(msgObj instanceof Map<?, ?> msg)) continue;

            Map<String, Object> key = (Map<String, Object>) msg.get("key");
            if (key == null) continue;

            Boolean fromMe = (Boolean) key.get("fromMe");
            if (Boolean.TRUE.equals(fromMe)) continue; // ignora mensagens enviadas por nós

            String remoteJid = (String) key.get("remoteJid");
            String messageId = (String) key.get("id");
            String pushName = (String) msg.get("pushName");

            log.info("[MENSAGEM RECEBIDA] instance={} remoteJid={} pushName={} messageId={}",
                    payload.getInstance(), remoteJid, pushName, messageId);

            String conteudo = null;
            String tipo = "TEXTO";
            Map<String, Object> messageContent = (Map<String, Object>) msg.get("message");
            if (messageContent != null) {
                if (messageContent.containsKey("conversation")) {
                    conteudo = (String) messageContent.get("conversation");
                    tipo = "TEXTO";
                } else if (messageContent.containsKey("extendedTextMessage")) {
                    Map<String, Object> ext = (Map<String, Object>) messageContent.get("extendedTextMessage");
                    conteudo = (String) ext.get("text");
                    tipo = "TEXTO";
                } else if (messageContent.containsKey("imageMessage")) {
                    Map<String, Object> img = (Map<String, Object>) messageContent.get("imageMessage");
                    conteudo = (String) img.get("caption");
                    tipo = "IMAGEM";
                } else if (messageContent.containsKey("audioMessage")) {
                    tipo = "AUDIO";
                } else if (messageContent.containsKey("videoMessage")) {
                    tipo = "VIDEO";
                } else if (messageContent.containsKey("documentMessage")) {
                    Map<String, Object> doc = (Map<String, Object>) messageContent.get("documentMessage");
                    conteudo = (String) doc.get("fileName");
                    tipo = "DOCUMENTO";
                }
            }

            mensagemService.registrarRecebida(payload.getInstance(), remoteJid, pushName, messageId, conteudo, tipo);
        }
    }
}
