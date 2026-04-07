package br.tec.dev2b.whats.infra.webhook;

import br.tec.dev2b.whats.infra.evolution.EvolutionApiClient;
import br.tec.dev2b.whats.infra.evolution.dto.GetBase64Response;
import br.tec.dev2b.whats.infra.minio.MinioUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Map;

/**
 * Baixa a mídia de uma mensagem WhatsApp via Evolution API (base64)
 * e faz upload para o MinIO, retornando a URL pública.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaDownloadService {

    /** Mapeamento de MIME type → extensão de arquivo. */
    private static final Map<String, String> MIME_PARA_EXTENSAO = Map.ofEntries(
            Map.entry("image/jpeg",                                                          "jpg"),
            Map.entry("image/png",                                                           "png"),
            Map.entry("image/gif",                                                           "gif"),
            Map.entry("image/webp",                                                          "webp"),
            Map.entry("audio/ogg",                                                           "ogg"),
            Map.entry("audio/mpeg",                                                          "mp3"),
            Map.entry("audio/mp4",                                                           "m4a"),
            Map.entry("video/mp4",                                                           "mp4"),
            Map.entry("video/mpeg",                                                          "mpeg"),
            Map.entry("application/pdf",                                                     "pdf"),
            Map.entry("application/msword",                                                  "doc"),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
            Map.entry("application/vnd.ms-excel",                                            "xls"),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",   "xlsx")
    );

    private final EvolutionApiClient evolutionApiClient;
    private final MinioUploadService  minioUploadService;

    /**
     * Baixa a mídia da Evolution API e faz upload para o MinIO.
     *
     * @param instanceName nome da instância Evolution API
     * @param messageId    ID da mensagem no WhatsApp (campo {@code key.id})
     * @param mimeTypeHint MIME type extraído do payload (fallback caso a API não informe)
     * @param nomeArquivo  nome original do arquivo (usado para inferir extensão)
     * @return URL pública no MinIO, ou {@code null} em caso de falha não crítica
     */
    @Nullable
    public String baixarEFazerUpload(String instanceName, String messageId,
                                     String mimeTypeHint, String nomeArquivo) {
        try {
            GetBase64Response response = evolutionApiClient.getBase64FromMedia(instanceName, messageId);

            if (response == null || response.getBase64() == null) {
                log.warn("[MEDIA] Evolution API retornou resposta vazia messageId={}", messageId);
                return null;
            }

            String base64Raw = response.getBase64();

            // Remove prefixo "data:image/jpeg;base64," se presente
            if (base64Raw.contains(",")) {
                base64Raw = base64Raw.substring(base64Raw.indexOf(',') + 1);
            }

            // Prefere o MIME informado pela API; usa hint do payload como fallback
            String mimeType = response.getMimetype() != null ? response.getMimetype() : mimeTypeHint;
            if (mimeType == null) mimeType = "application/octet-stream";

            // Normaliza "audio/ogg; codecs=opus" → "audio/ogg"
            String mimeBase = mimeType.contains(";")
                    ? mimeType.substring(0, mimeType.indexOf(';')).trim()
                    : mimeType;

            String extensao = MIME_PARA_EXTENSAO.getOrDefault(mimeBase, extensaoDePorNome(nomeArquivo));

            byte[] bytes = Base64.getDecoder().decode(base64Raw.trim());
            try (var is = new ByteArrayInputStream(bytes)) {
                String url = minioUploadService.upload(is, mimeBase, extensao);
                log.info("[MEDIA] Upload concluído messageId={} mime={} tamanho={}B url={}",
                        messageId, mimeBase, bytes.length, url);
                return url;
            }

        } catch (Exception e) {
            log.warn("[MEDIA] Falha ao processar mídia messageId={} instance={}: {}",
                    messageId, instanceName, e.getMessage());
            return null;
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private String extensaoDePorNome(String nomeArquivo) {
        if (nomeArquivo == null) return "bin";
        int dot = nomeArquivo.lastIndexOf('.');
        return dot >= 0 ? nomeArquivo.substring(dot + 1).toLowerCase() : "bin";
    }
}
