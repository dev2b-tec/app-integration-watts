package br.tec.dev2b.whats.mensagem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.UUID;

/**
 * DTO unificado para envio de todos os tipos de mensagem via WhatsApp.
 *
 * Discriminado pelo campo {@code tipo}:
 * <ul>
 *   <li>TEXTO      – preencher {@code texto}</li>
 *   <li>IMAGEM     – preencher {@code mediaUrl} (url ou base64) + opcional {@code caption}, {@code mimeType}</li>
 *   <li>VIDEO      – preencher {@code mediaUrl} + opcional {@code caption}, {@code fileName}, {@code mimeType}</li>
 *   <li>DOCUMENTO  – preencher {@code mediaUrl} + {@code fileName} + {@code mimeType}</li>
 *   <li>AUDIO_PTT  – preencher {@code audioUrl} (url ou base64 de áudio ogg/mp4)</li>
 *   <li>STICKER    – preencher {@code stickerUrl} (url ou base64 WebP)</li>
 *   <li>LOCALIZACAO – preencher {@code latitude}, {@code longitude}, {@code locNome}, {@code locEndereco}</li>
 *   <li>REACAO     – preencher {@code reacaoEmoji}, {@code reacaoMsgId}, {@code reacaoRemoteJid}</li>
 * </ul>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnviarMensagemDto {

    private UUID instanciaId;

    /** Número no formato internacional: 5581999990000 */
    private String numero;

    /**
     * Tipo da mensagem.
     * Valores: TEXTO | IMAGEM | VIDEO | DOCUMENTO | AUDIO_PTT | STICKER | LOCALIZACAO | REACAO
     */
    private String tipo = "TEXTO";

    // ── TEXTO ─────────────────────────────────────────────────────────────────
    private String texto;

    // ── MÍDIA (IMAGEM / VIDEO / DOCUMENTO) ────────────────────────────────────
    /** URL pública ou string Base64 do arquivo */
    private String mediaUrl;
    /** Legenda exibida abaixo da mídia */
    private String caption;
    /** Nome do arquivo (obrigatório para DOCUMENTO) */
    private String fileName;
    /** MIME type, ex: image/jpeg, video/mp4, application/pdf */
    private String mimeType;

    // ── ÁUDIO PTT ─────────────────────────────────────────────────────────────
    /** URL pública ou Base64 do arquivo de áudio (ogg/mp4) */
    private String audioUrl;

    // ── STICKER ───────────────────────────────────────────────────────────────
    /** URL pública ou Base64 do sticker (WebP) */
    private String stickerUrl;

    // ── LOCALIZAÇÃO ───────────────────────────────────────────────────────────
    private Double latitude;
    private Double longitude;
    private String locNome;
    private String locEndereco;

    // ── REAÇÃO ────────────────────────────────────────────────────────────────
    /** Emoji da reação, ex: "❤️" */
    private String reacaoEmoji;
    /** message_id da mensagem alvo da reação */
    private String reacaoMsgId;
    /** remoteJid da mensagem alvo */
    private String reacaoRemoteJid;
    /** fromMe da mensagem alvo */
    private Boolean reacaoFromMe;

    // ── Delay opcional ────────────────────────────────────────────────────────
    /** Atraso em ms antes de enviar (simula digitação) */
    private Integer delay;
}
