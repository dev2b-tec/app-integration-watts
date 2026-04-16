package br.tec.dev2b.whats.conversa.scheduler;

import br.tec.dev2b.whats.conversa.model.Conversa;
import br.tec.dev2b.whats.conversa.model.StatusConversa;
import br.tec.dev2b.whats.conversa.repository.ConversaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Expira conversas abertas há mais de 24 horas.
 *
 * Regras:
 *  · EM_ESPERA  criadas há > 24 h  →  FINALIZADA
 *  · ATIVA / EM_ATENDIMENTO / EM_CHATBOT criadas há > 24 h  →  FINALIZADA
 *
 * Roda a cada hora para não deixar estado residual acumular.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConversaExpiracaoScheduler {

    private static final long HORAS_EXPIRACAO = 24;

    private final ConversaRepository conversaRepository;

    @Scheduled(fixedDelay = 60 * 60 * 1000L) // 1 h
    @Transactional
    public void expirarConversas() {
        LocalDateTime limite = LocalDateTime.now().minusHours(HORAS_EXPIRACAO);

        List<StatusConversa> statusExpiram = List.of(
                StatusConversa.EM_ESPERA,
                StatusConversa.ATIVA,
                StatusConversa.EM_ATENDIMENTO,
                StatusConversa.EM_CHATBOT
        );

        List<Conversa> expiradas = conversaRepository
                .findByStatusInAndCreatedAtBefore(statusExpiram, limite);

        if (expiradas.isEmpty()) return;

        for (Conversa c : expiradas) {
            c.setStatus(StatusConversa.FINALIZADA);
        }
        conversaRepository.saveAll(expiradas);

        log.info("[SCHEDULER] {} conversa(s) expirada(s) após {}h de inatividade",
                expiradas.size(), HORAS_EXPIRACAO);
    }
}
