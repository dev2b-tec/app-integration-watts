package br.tec.dev2b.whats.infra.webhook;

import br.tec.dev2b.whats.conversa.service.ConversaWebhookService;
import br.tec.dev2b.whats.chatbot.service.ChatbotMotorService;
import br.tec.dev2b.whats.infra.evolution.dto.WebhookPayload;
import br.tec.dev2b.whats.instancia.model.Instancia;
import br.tec.dev2b.whats.instancia.repository.InstanciaRepository;
import br.tec.dev2b.whats.mensagem.service.MensagemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Encapsula o processamento do evento {@code messages.upsert} da Evolution API.
 *
 * <p>Responsabilidades:
 * <ol>
 *   <li>Normalizar o payload (objeto único ou lista de mensagens)</li>
 *   <li>Extrair campos relevantes de cada mensagem</li>
 *   <li>Registrar na tabela legada {@code mensagens} via {@link MensagemService}</li>
 *   <li>Vincular à {@code Conversa} aberta via {@link ConversaWebhookService}</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MensagemUpsertService {

    private final InstanciaRepository instanciaRepository;
    private final MensagemService     mensagemService;
    private final ConversaWebhookService conversaWebhookService;
    private final MediaDownloadService mediaDownloadService;
    private final ChatbotMotorService chatbotMotorService;

    // ─────────────────────────────────────────────────────────────────────────

    public void processar(WebhookPayload payload) {
        Map<String, Object> data = payload.getData();
        if (data == null) return;

        List<?> mensagens = normalizarLista(data);

        Instancia instancia = instanciaRepository
                .findByInstanceName(payload.getInstance())
                .orElse(null);

        for (Object msgObj : mensagens) {
            if (!(msgObj instanceof Map<?, ?> msg)) continue;
            //noinspection unchecked
            processarMensagem(payload.getInstance(), instancia, (Map<String, Object>) msg);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void processarMensagem(String instanceName, Instancia instancia, Map<String, Object> msg) {

        Map<String, Object> key = (Map<String, Object>) msg.get("key");
        if (key == null) return;

        boolean fromMe   = Boolean.TRUE.equals(key.get("fromMe"));
        String remoteJid = (String) key.get("remoteJid");
        String messageId = (String) key.get("id");
        String pushName  = (String) msg.get("pushName");
        long   timestamp = toLong(msg.get("messageTimestamp"));

        if (remoteJid == null) return;

        String telefone = remoteJid.replaceAll("@.*$", "");

        // Normaliza celular BR de 12 para 13 dígitos: insere '9' na posição 5
        // Ex: 558196349077 → 5581996349077
        if (telefone.length() == 12) {
            telefone = telefone.substring(0, 4) + "9" + telefone.substring(4);
        }

        ConteudoMensagem conteudo = extrairConteudo(
                (Map<String, Object>) msg.get("message"));

        // Baixa mídia da Evolution API e sobe no MinIO quando aplicável
        String urlArquivo = null;
        if (conteudo.temMidia() && instancia != null) {
            urlArquivo = mediaDownloadService.baixarEFazerUpload(
                    instanceName, messageId, conteudo.mimeType(), conteudo.nomeArquivo());
        }

        // 1 — tabela legada mensagens (histórico por instância)
        if (instancia != null) {
            if (fromMe) {
                mensagemService.registrarEnviada(
                        instanceName, remoteJid, messageId, conteudo.texto(), conteudo.tipo());
            } else {
                mensagemService.registrarRecebida(
                        instanceName, remoteJid, pushName, messageId, conteudo.texto(), conteudo.tipo());
            }
        }

        // 2 — conversa aberta (ATIVA ou EM_ATENDIMENTO)
        if (instancia != null && instancia.getEmpresaId() != null) {
            conversaWebhookService.processarMensagem(
                    instancia.getEmpresaId(), telefone, conteudo.texto(), timestamp, fromMe, urlArquivo);
        } else {
            log.debug("[UPSERT] Instância sem empresaId instanceName={}", instanceName);
        }

        // 3 — motor do chatbot (somente mensagens recebidas)
        if (!fromMe && instancia != null && instancia.getEmpresaId() != null) {
            try {
                chatbotMotorService.processar(instanceName, instancia.getEmpresaId(),
                        telefone, conteudo.texto(), pushName);
            } catch (Exception e) {
                log.error("[UPSERT] Erro no motor chatbot para telefone={}: {}", telefone, e.getMessage(), e);
            }
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    /** Normaliza payload que pode chegar como objeto único ou como lista. */
    @SuppressWarnings("unchecked")
    private List<?> normalizarLista(Map<String, Object> data) {
        Object messagesObj = data.get("messages");
        return messagesObj instanceof List<?> list ? list : List.of(data);
    }

    @SuppressWarnings("unchecked")
    private ConteudoMensagem extrairConteudo(Map<String, Object> messageContent) {
        if (messageContent == null) return new ConteudoMensagem(null, "TEXTO");

        if (messageContent.containsKey("conversation")) {
            return new ConteudoMensagem((String) messageContent.get("conversation"), "TEXTO");
        }
        if (messageContent.containsKey("extendedTextMessage")) {
            Map<String, Object> ext = (Map<String, Object>) messageContent.get("extendedTextMessage");
            return new ConteudoMensagem((String) ext.get("text"), "TEXTO");
        }
        if (messageContent.containsKey("imageMessage")) {
            Map<String, Object> img = (Map<String, Object>) messageContent.get("imageMessage");
            return new ConteudoMensagem((String) img.get("caption"), "IMAGEM",
                    (String) img.get("mimetype"), null);
        }
        if (messageContent.containsKey("audioMessage")) {
            Map<String, Object> audio = (Map<String, Object>) messageContent.get("audioMessage");
            return new ConteudoMensagem(null, "AUDIO",
                    (String) audio.get("mimetype"), null);
        }
        if (messageContent.containsKey("videoMessage")) {
            Map<String, Object> video = (Map<String, Object>) messageContent.get("videoMessage");
            return new ConteudoMensagem((String) video.get("caption"), "VIDEO",
                    (String) video.get("mimetype"), null);
        }
        if (messageContent.containsKey("documentMessage")) {
            Map<String, Object> doc = (Map<String, Object>) messageContent.get("documentMessage");
            return new ConteudoMensagem((String) doc.get("fileName"), "DOCUMENTO",
                    (String) doc.get("mimetype"), (String) doc.get("fileName"));
        }
        return new ConteudoMensagem(null, "TEXTO");
    }

    private long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
        }
        return Instant.now().getEpochSecond();
    }

    /** Tupla imutável com texto, tipo, MIME type e nome do arquivo da mensagem. */
    private record ConteudoMensagem(String texto, String tipo, String mimeType, String nomeArquivo) {
        ConteudoMensagem(String texto, String tipo) {
            this(texto, tipo, null, null);
        }
        boolean temMidia() {
            return !"TEXTO".equals(tipo);
        }
    }
}
