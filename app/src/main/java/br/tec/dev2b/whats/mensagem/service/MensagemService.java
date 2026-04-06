package br.tec.dev2b.whats.mensagem.service;

import br.tec.dev2b.whats.infra.evolution.EvolutionApiClient;
import br.tec.dev2b.whats.infra.evolution.dto.EnviarTextoRequest;
import br.tec.dev2b.whats.infra.websocket.WebSocketNotificationService;
import br.tec.dev2b.whats.instancia.model.Instancia;
import br.tec.dev2b.whats.instancia.repository.InstanciaRepository;
import br.tec.dev2b.whats.mensagem.dto.EnviarMensagemDto;
import br.tec.dev2b.whats.mensagem.dto.MensagemDto;
import br.tec.dev2b.whats.mensagem.model.Mensagem;
import br.tec.dev2b.whats.mensagem.repository.MensagemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MensagemService {

    private final MensagemRepository mensagemRepository;
    private final InstanciaRepository instanciaRepository;
    private final EvolutionApiClient evolutionApiClient;
    private final WebSocketNotificationService webSocketService;

    @Transactional
    public MensagemDto enviarTexto(EnviarMensagemDto dto) {
        Instancia instancia = instanciaRepository.findById(dto.getInstanciaId())
                .orElseThrow(() -> new IllegalArgumentException("Instância não encontrada: " + dto.getInstanciaId()));

        if (!"CONECTADA".equals(instancia.getStatus()) && !"ABERTA".equals(instancia.getStatus())) {
            throw new IllegalStateException("Instância não está conectada. Status atual: " + instancia.getStatus());
        }

        // Monta request para Evolution API
        EnviarTextoRequest apiRequest = new EnviarTextoRequest();
        apiRequest.setNumber(dto.getNumero());
        apiRequest.setText(dto.getTexto());

        evolutionApiClient.enviarTexto(instancia.getInstanceName(), apiRequest);

        String remoteJid = dto.getNumero() + "@s.whatsapp.net";

        Mensagem mensagem = Mensagem.builder()
                .instancia(instancia)
                .remoteJid(remoteJid)
                .numero(dto.getNumero())
                .tipo("TEXTO")
                .conteudo(dto.getTexto())
                .direcao("ENVIADA")
                .statusEnvio("ENVIADA")
                .build();

        mensagem = mensagemRepository.save(mensagem);

        MensagemDto result = MensagemDto.from(mensagem);
        webSocketService.notificarMensagem(instancia.getId(), result);
        return result;
    }

    @Transactional(readOnly = true)
    public Page<MensagemDto> listar(UUID instanciaId, Pageable pageable) {
        return mensagemRepository.findByInstanciaIdOrderByCreatedAtDesc(instanciaId, pageable)
                .map(MensagemDto::from);
    }

    @Transactional(readOnly = true)
    public List<MensagemDto> listarConversa(UUID instanciaId, String remoteJid) {
        return mensagemRepository.findByInstanciaIdAndRemoteJidOrderByCreatedAtAsc(instanciaId, remoteJid)
                .stream().map(MensagemDto::from).toList();
    }

    @Transactional(readOnly = true)
    public MensagemDto buscarPorId(UUID id) {
        return mensagemRepository.findById(id)
                .map(MensagemDto::from)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada: " + id));
    }

    /**
     * Registra mensagem recebida via webhook da Evolution API.
     * Chamado pelo WebhookController.
     */
    @Transactional
    public MensagemDto registrarRecebida(String instanceName, String remoteJid, String pushName,
                                          String messageId, String conteudo, String tipo) {
        Instancia instancia = instanciaRepository.findByInstanceName(instanceName)
                .orElse(null);

        if (instancia == null) return null;

        String numero = remoteJid.replace("@s.whatsapp.net", "").replace("@g.us", "");

        Mensagem mensagem = Mensagem.builder()
                .instancia(instancia)
                .remoteJid(remoteJid)
                .numero(numero)
                .pushName(pushName)
                .messageId(messageId)
                .tipo(tipo != null ? tipo : "TEXTO")
                .conteudo(conteudo)
                .direcao("RECEBIDA")
                .statusEnvio("ENTREGUE")
                .build();

        mensagem = mensagemRepository.save(mensagem);

        log.info("[MENSAGEM SALVA] id={} instancia={} numero={} tipo={} pushName={} conteudo={}",
                mensagem.getId(), instanceName, numero, mensagem.getTipo(), pushName,
                conteudo != null ? conteudo.substring(0, Math.min(conteudo.length(), 100)) : "(sem conteúdo)");

        MensagemDto result = MensagemDto.from(mensagem);
        webSocketService.notificarMensagem(instancia.getId(), result);
        return result;
    }
}
