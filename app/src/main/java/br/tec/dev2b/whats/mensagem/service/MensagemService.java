package br.tec.dev2b.whats.mensagem.service;

import br.tec.dev2b.whats.infra.evolution.EvolutionApiClient;
import br.tec.dev2b.whats.infra.evolution.dto.*;
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

    // ─────────────────────────────────────────────────────────────────────────
    // MÍDIA (imagem / vídeo / documento)
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public MensagemDto enviarMidia(EnviarMensagemDto dto) {
        Instancia instancia = validarInstancia(dto.getInstanciaId());

        String mediatype = switch (dto.getTipo().toUpperCase()) {
            case "VIDEO"    -> "video";
            case "DOCUMENTO" -> "document";
            default          -> "image";
        };

        EnviarMidiaRequest req = new EnviarMidiaRequest();
        req.setNumber(dto.getNumero());
        req.setMediatype(mediatype);
        req.setMimetype(dto.getMimeType());
        req.setCaption(dto.getCaption());
        req.setMedia(dto.getMediaUrl());
        req.setFileName(dto.getFileName());
        req.setDelay(dto.getDelay());

        evolutionApiClient.enviarMidia(instancia.getInstanceName(), req);

        return salvarESinalizar(instancia, dto.getNumero(), dto.getTipo().toUpperCase(),
                dto.getCaption() != null ? dto.getCaption() : dto.getFileName(),
                dto.getMediaUrl());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ÁUDIO PTT (nota de voz)
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public MensagemDto enviarAudio(EnviarMensagemDto dto) {
        Instancia instancia = validarInstancia(dto.getInstanciaId());

        EnviarAudioRequest req = new EnviarAudioRequest();
        req.setNumber(dto.getNumero());
        req.setAudio(dto.getAudioUrl());
        req.setDelay(dto.getDelay());

        evolutionApiClient.enviarAudio(instancia.getInstanceName(), req);

        return salvarESinalizar(instancia, dto.getNumero(), "AUDIO_PTT", null, dto.getAudioUrl());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STICKER
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public MensagemDto enviarSticker(EnviarMensagemDto dto) {
        Instancia instancia = validarInstancia(dto.getInstanciaId());

        EnviarStickerRequest req = new EnviarStickerRequest();
        req.setNumber(dto.getNumero());
        req.setSticker(dto.getStickerUrl());
        req.setDelay(dto.getDelay());

        evolutionApiClient.enviarSticker(instancia.getInstanceName(), req);

        return salvarESinalizar(instancia, dto.getNumero(), "STICKER", null, dto.getStickerUrl());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOCALIZAÇÃO
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public MensagemDto enviarLocalizacao(EnviarMensagemDto dto) {
        Instancia instancia = validarInstancia(dto.getInstanciaId());

        EnviarLocalizacaoRequest req = new EnviarLocalizacaoRequest();
        req.setNumber(dto.getNumero());
        req.setName(dto.getLocNome());
        req.setAddress(dto.getLocEndereco());
        req.setLatitude(dto.getLatitude());
        req.setLongitude(dto.getLongitude());
        req.setDelay(dto.getDelay());

        evolutionApiClient.enviarLocalizacao(instancia.getInstanceName(), req);

        String conteudo = dto.getLocNome() + " | " + dto.getLatitude() + "," + dto.getLongitude();
        return salvarESinalizar(instancia, dto.getNumero(), "LOCALIZACAO", conteudo, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REAÇÃO
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public void enviarReacao(EnviarMensagemDto dto) {
        Instancia instancia = validarInstancia(dto.getInstanciaId());

        EnviarReacaoRequest.Key key = new EnviarReacaoRequest.Key();
        key.setRemoteJid(dto.getReacaoRemoteJid());
        key.setFromMe(dto.getReacaoFromMe());
        key.setId(dto.getReacaoMsgId());

        EnviarReacaoRequest req = new EnviarReacaoRequest();
        req.setKey(key);
        req.setReaction(dto.getReacaoEmoji());

        evolutionApiClient.enviarReacao(instancia.getInstanceName(), req);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers privados
    // ─────────────────────────────────────────────────────────────────────────
    private Instancia validarInstancia(UUID instanciaId) {
        Instancia instancia = instanciaRepository.findById(instanciaId)
                .orElseThrow(() -> new IllegalArgumentException("Instância não encontrada: " + instanciaId));
        if (!"CONECTADA".equals(instancia.getStatus()) && !"ABERTA".equals(instancia.getStatus())) {
            throw new IllegalStateException("Instância não está conectada. Status: " + instancia.getStatus());
        }
        return instancia;
    }

    private MensagemDto salvarESinalizar(Instancia instancia, String numero,
                                          String tipo, String conteudo, String mediaUrl) {
        String remoteJid = numero + "@s.whatsapp.net";
        Mensagem mensagem = Mensagem.builder()
                .instancia(instancia)
                .remoteJid(remoteJid)
                .numero(numero)
                .tipo(tipo)
                .conteudo(conteudo)
                .mediaUrl(mediaUrl)
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

    /**
     * Registra mensagem enviada por nós via webhook da Evolution API.
     * Chamado pelo MensagemUpsertService quando fromMe=true.
     */
    @Transactional
    public MensagemDto registrarEnviada(String instanceName, String remoteJid,
                                        String messageId, String conteudo, String tipo) {
        Instancia instancia = instanciaRepository.findByInstanceName(instanceName)
                .orElse(null);

        if (instancia == null) return null;

        String numero = remoteJid.replace("@s.whatsapp.net", "").replace("@g.us", "");

        Mensagem mensagem = Mensagem.builder()
                .instancia(instancia)
                .remoteJid(remoteJid)
                .numero(numero)
                .messageId(messageId)
                .tipo(tipo != null ? tipo : "TEXTO")
                .conteudo(conteudo)
                .direcao("ENVIADA")
                .statusEnvio("ENVIADA")
                .build();

        mensagem = mensagemRepository.save(mensagem);

        log.info("[MENSAGEM SALVA] id={} instancia={} numero={} tipo={} direcao=ENVIADA conteudo={}",
                mensagem.getId(), instanceName, numero, mensagem.getTipo(),
                conteudo != null ? conteudo.substring(0, Math.min(conteudo.length(), 100)) : "(sem conteúdo)");

        MensagemDto result = MensagemDto.from(mensagem);
        webSocketService.notificarMensagem(instancia.getId(), result);
        return result;
    }
}
