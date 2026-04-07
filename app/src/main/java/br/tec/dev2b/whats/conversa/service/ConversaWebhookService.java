package br.tec.dev2b.whats.conversa.service;

import br.tec.dev2b.whats.conversa.dto.MensagemDaConversaDto;
import br.tec.dev2b.whats.conversa.model.Conversa;
import br.tec.dev2b.whats.conversa.model.MensagemDaConversa;
import br.tec.dev2b.whats.conversa.model.StatusConversa;
import br.tec.dev2b.whats.conversa.repository.ConversaRepository;
import br.tec.dev2b.whats.conversa.repository.MensagemDaConversaRepository;
import br.tec.dev2b.whats.infra.websocket.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Processa mensagens recebidas via webhook e as vincula à Conversa
 * correspondente, caso ela exista com status ATIVA ou EM_ATENDIMENTO.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConversaWebhookService {

    private static final List<StatusConversa> STATUS_ACEITAM_MENSAGEM =
            List.of(StatusConversa.ATIVA, StatusConversa.EM_ATENDIMENTO);

    private final ConversaRepository conversaRepository;
    private final MensagemDaConversaRepository mensagemDaConversaRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    /**
     * Tenta vincular a mensagem a uma conversa aberta.
     *
     * @param empresaId   UUID da empresa dona da instância
     * @param telefone    número limpo (ex: "5581996349077")
     * @param texto       conteúdo textual da mensagem (pode ser null para mídia)
     * @param timestamp   epoch-seconds do campo messageTimestamp do payload
     * @param fromMe      true quando a mensagem foi enviada por nós
     * @param urlArquivo  URL da mídia no MinIO (null para mensagens de texto)
     * @return true se a mensagem foi salva numa conversa, false se não havia conversa aberta
     */
    @Transactional
    public boolean processarMensagem(UUID empresaId, String telefone,
                                     String texto, long timestamp, boolean fromMe,
                                     String urlArquivo) {

        Optional<Conversa> opt = conversaRepository
                .findByEmpresaIdAndTelefoneAndStatusIn(empresaId, telefone, STATUS_ACEITAM_MENSAGEM);

        if (opt.isEmpty()) {
            log.debug("[CONVERSA] Nenhuma conversa aberta para empresaId={} telefone={} – mensagem descartada",
                    empresaId, telefone);
            return false;
        }

        Conversa conversa = opt.get();
        Instant recebidaEm = Instant.ofEpochSecond(timestamp);

        MensagemDaConversa msg = MensagemDaConversa.builder()
                .conversa(conversa)
                .texto(texto)
                .recebidaEm(recebidaEm)
                .urlArquivo(urlArquivo)
                .enviada(fromMe)
                .build();

        mensagemDaConversaRepository.save(msg);

        webSocketNotificationService.notificarMensagemDaConversa(
                conversa.getId(),
                MensagemDaConversaDto.from(msg)
        );

        log.info("[CONVERSA] Mensagem salva conversaId={} fromMe={} telefone={} texto={}",
                conversa.getId(), fromMe, telefone,
                texto != null ? texto.substring(0, Math.min(texto.length(), 80)) : "(sem texto)");

        return true;
    }
}
