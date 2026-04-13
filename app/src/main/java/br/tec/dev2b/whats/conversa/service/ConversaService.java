package br.tec.dev2b.whats.conversa.service;

import br.tec.dev2b.whats.conversa.dto.*;
import br.tec.dev2b.whats.conversa.model.Conversa;
import br.tec.dev2b.whats.conversa.model.MensagemDaConversa;
import br.tec.dev2b.whats.conversa.model.StatusConversa;
import br.tec.dev2b.whats.conversa.repository.ConversaRepository;
import br.tec.dev2b.whats.conversa.repository.MensagemDaConversaRepository;
import br.tec.dev2b.whats.infra.evolution.EvolutionApiClient;
import br.tec.dev2b.whats.infra.evolution.dto.*;
import br.tec.dev2b.whats.infra.minio.MinioUploadService;
import br.tec.dev2b.whats.instancia.model.Instancia;
import br.tec.dev2b.whats.instancia.repository.InstanciaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversaService {

    private final ConversaRepository conversaRepository;
    private final MensagemDaConversaRepository mensagemRepository;
    private final MinioUploadService minioUploadService;
    private final EvolutionApiClient evolutionApiClient;
    private final InstanciaRepository instanciaRepository;

    // ──────────────────────────────────────────────────────────────────────────
    // Conversa
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional
    public ConversaDto criar(CriarConversaDto dto) {
        Conversa conversa = Conversa.builder()
                .empresaId(dto.empresaId())
                .telefone(dto.telefone())
                .nome(dto.nome())
                .clienteId(dto.clienteId())
                .build();
        return ConversaDto.resumo(conversaRepository.save(conversa));
    }

    /** Busca conversa ativa/em-espera para o telefone ou cria uma nova. */
    @Transactional
    public ConversaDto buscarOuCriar(UUID empresaId, String telefone, String nome, UUID clienteId) {
        return conversaRepository
                .findByEmpresaIdAndTelefoneAndStatusNot(empresaId, telefone, StatusConversa.FINALIZADA)
                .map(ConversaDto::resumo)
                .orElseGet(() -> criar(new CriarConversaDto(empresaId, telefone, nome, clienteId)));
    }

    @Transactional(readOnly = true)
    public ConversaDto buscarPorId(UUID id) {
        return ConversaDto.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<ConversaDto> listarPorEmpresa(UUID empresaId, Pageable pageable) {
        return conversaRepository
                .findByEmpresaIdOrderByCreatedAtDesc(empresaId, pageable)
                .map(ConversaDto::resumo);
    }

    @Transactional(readOnly = true)
    public List<ConversaDto> listarPorEmpresaEStatus(UUID empresaId, StatusConversa status) {
        return conversaRepository
                .findByEmpresaIdAndStatus(empresaId, status)
                .stream()
                .map(ConversaDto::resumo)
                .toList();
    }

    @Transactional
    public ConversaDto alterarStatus(UUID id, AlterarStatusConversaDto dto) {
        Conversa conversa = findOrThrow(id);
        conversa.setStatus(dto.status());
        return ConversaDto.resumo(conversaRepository.save(conversa));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Mensagens da conversa
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Adiciona uma mensagem de texto a uma conversa e, se {@code enviada=true},
     * dispara o envio pelo WhatsApp via Evolution API.
     */
    @Transactional
    public MensagemDaConversaDto adicionarMensagemTexto(UUID conversaId, String texto, Instant recebidaEm, boolean enviada) {
        Conversa conversa = findOrThrow(conversaId);

        if (enviada) {
            findInstanciaAtiva(conversa.getEmpresaId()).ifPresent(inst -> {
                EnviarTextoRequest req = new EnviarTextoRequest();
                req.setNumber(conversa.getTelefone());
                req.setText(texto);
                try {
                    evolutionApiClient.enviarTexto(inst.getInstanceName(), req);
                } catch (Exception e) {
                    log.warn("[ConversaService] Falha ao enviar texto via Evolution API: {}", e.getMessage());
                }
            });
        }

        MensagemDaConversa msg = MensagemDaConversa.builder()
                .conversa(conversa)
                .texto(texto)
                .tipo("TEXTO")
                .recebidaEm(recebidaEm != null ? recebidaEm : Instant.now())
                .enviada(enviada)
                .build();
        return MensagemDaConversaDto.from(mensagemRepository.save(msg));
    }

    /**
     * Adiciona uma mensagem com arquivo: faz upload ao MinIO e envia o link
     * pelo WhatsApp via Evolution API (image/video/document).
     */
    @Transactional
    public MensagemDaConversaDto adicionarMensagemComArquivo(
            UUID conversaId,
            String texto,
            Instant recebidaEm,
            MultipartFile arquivo) {

        Conversa conversa = findOrThrow(conversaId);

        String extensao = obterExtensao(arquivo.getOriginalFilename());
        String urlArquivo = minioUploadService.upload(
                getInputStream(arquivo),
                arquivo.getContentType() != null ? arquivo.getContentType() : "application/octet-stream",
                extensao
        );

        String mimeType = arquivo.getContentType() != null ? arquivo.getContentType() : "application/octet-stream";
        String tipo = detectarTipo(mimeType);

        // Evolution API requer URL pública ou base64. Como o MinIO pode ser interno,
        // enviamos base64 e guardamos a URL do MinIO apenas para exibição no chat.
        final String base64Midia = toBase64(arquivo);
        findInstanciaAtiva(conversa.getEmpresaId()).ifPresent(inst -> {
            EnviarMidiaRequest req = new EnviarMidiaRequest();
            req.setNumber(conversa.getTelefone());
            req.setMediatype(mapearMediatype(tipo));
            req.setMimetype(mimeType);
            req.setCaption(texto);
            req.setMedia(base64Midia);
            req.setFileName(arquivo.getOriginalFilename());
            try {
                evolutionApiClient.enviarMidia(inst.getInstanceName(), req);
            } catch (Exception e) {
                log.warn("[ConversaService] Falha ao enviar mídia via Evolution API: {}", e.getMessage());
            }
        });

        MensagemDaConversa msg = MensagemDaConversa.builder()
                .conversa(conversa)
                .texto(texto)
                .tipo(tipo)
                .nomeArquivo(arquivo.getOriginalFilename())
                .recebidaEm(recebidaEm != null ? recebidaEm : Instant.now())
                .urlArquivo(urlArquivo)
                .enviada(true)
                .build();

        return MensagemDaConversaDto.from(mensagemRepository.save(msg));
    }

    /**
     * Faz upload de áudio ao MinIO e envia como nota de voz (PTT) via Evolution API.
     */
    @Transactional
    public MensagemDaConversaDto adicionarMensagemAudio(UUID conversaId, MultipartFile audio) {
        Conversa conversa = findOrThrow(conversaId);

        String urlAudio = minioUploadService.upload(
                getInputStream(audio),
                audio.getContentType() != null ? audio.getContentType() : "audio/ogg",
                obterExtensao(audio.getOriginalFilename())
        );

        // Evolution API requer URL pública ou base64. Enviamos base64.
        final String base64Audio = toBase64(audio);
        findInstanciaAtiva(conversa.getEmpresaId()).ifPresent(inst -> {
            EnviarAudioRequest req = new EnviarAudioRequest();
            req.setNumber(conversa.getTelefone());
            req.setAudio(base64Audio);
            try {
                evolutionApiClient.enviarAudio(inst.getInstanceName(), req);
            } catch (Exception e) {
                log.warn("[ConversaService] Falha ao enviar áudio via Evolution API: {}", e.getMessage());
            }
        });

        MensagemDaConversa msg = MensagemDaConversa.builder()
                .conversa(conversa)
                .tipo("AUDIO_PTT")
                .nomeArquivo(audio.getOriginalFilename())
                .recebidaEm(Instant.now())
                .urlArquivo(urlAudio)
                .enviada(true)
                .build();

        return MensagemDaConversaDto.from(mensagemRepository.save(msg));
    }

    /**
     * Envia localização via Evolution API e salva na conversa.
     */
    @Transactional
    public MensagemDaConversaDto adicionarMensagemLocalizacao(UUID conversaId,
                                                               Double latitude, Double longitude,
                                                               String nome, String endereco) {
        Conversa conversa = findOrThrow(conversaId);

        findInstanciaAtiva(conversa.getEmpresaId()).ifPresent(inst -> {
            EnviarLocalizacaoRequest req = new EnviarLocalizacaoRequest();
            req.setNumber(conversa.getTelefone());
            req.setLatitude(latitude);
            req.setLongitude(longitude);
            req.setName(nome);
            req.setAddress(endereco);
            try {
                evolutionApiClient.enviarLocalizacao(inst.getInstanceName(), req);
            } catch (Exception e) {
                log.warn("[ConversaService] Falha ao enviar localização via Evolution API: {}", e.getMessage());
            }
        });

        String texto = nome != null ? nome : latitude + ", " + longitude;
        MensagemDaConversa msg = MensagemDaConversa.builder()
                .conversa(conversa)
                .tipo("LOCALIZACAO")
                .texto(texto)
                .latitude(latitude)
                .longitude(longitude)
                .recebidaEm(Instant.now())
                .enviada(true)
                .build();

        return MensagemDaConversaDto.from(mensagemRepository.save(msg));
    }

    @Transactional(readOnly = true)
    public Page<MensagemDaConversaDto> listarMensagens(UUID conversaId, Pageable pageable) {
        return mensagemRepository
                .findByConversaIdOrderByRecebidaEmAsc(conversaId, pageable)
                .map(MensagemDaConversaDto::from);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private Conversa findOrThrow(UUID id) {
        return conversaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada: " + id));
    }

    private Optional<Instancia> findInstanciaAtiva(UUID empresaId) {
        return instanciaRepository.findByEmpresaId(empresaId)
                .stream()
                .filter(i -> "CONECTADA".equals(i.getStatus()) || "ABERTA".equals(i.getStatus()))
                .findFirst();
    }

    private String obterExtensao(String nomeArquivo) {
        if (nomeArquivo != null && nomeArquivo.contains(".")) {
            return nomeArquivo.substring(nomeArquivo.lastIndexOf('.') + 1);
        }
        return "bin";
    }

    private String detectarTipo(String mimeType) {
        if (mimeType == null) return "DOCUMENTO";
        if (mimeType.startsWith("image/")) return "IMAGEM";
        if (mimeType.startsWith("video/")) return "VIDEO";
        if (mimeType.startsWith("audio/")) return "AUDIO_PTT";
        return "DOCUMENTO";
    }

    private String mapearMediatype(String tipo) {
        return switch (tipo) {
            case "VIDEO"    -> "video";
            case "DOCUMENTO" -> "document";
            default          -> "image";
        };
    }

    private java.io.InputStream getInputStream(MultipartFile arquivo) {
        try {
            return arquivo.getInputStream();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Erro ao ler arquivo enviado", e);
        }
    }

    /**
     * Converte um MultipartFile para string Base64 pura (sem prefixo data URI).
     * Usado para envio à Evolution API quando a URL do MinIO não é acessível externamente.
     */
    private String toBase64(MultipartFile arquivo) {
        try {
            return java.util.Base64.getEncoder().encodeToString(arquivo.getBytes());
        } catch (java.io.IOException e) {
            throw new RuntimeException("Erro ao converter arquivo para base64", e);
        }
    }
}
