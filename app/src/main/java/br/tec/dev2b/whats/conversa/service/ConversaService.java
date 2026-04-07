package br.tec.dev2b.whats.conversa.service;

import br.tec.dev2b.whats.conversa.dto.*;
import br.tec.dev2b.whats.conversa.model.Conversa;
import br.tec.dev2b.whats.conversa.model.MensagemDaConversa;
import br.tec.dev2b.whats.conversa.model.StatusConversa;
import br.tec.dev2b.whats.conversa.repository.ConversaRepository;
import br.tec.dev2b.whats.conversa.repository.MensagemDaConversaRepository;
import br.tec.dev2b.whats.infra.minio.MinioUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversaService {

    private final ConversaRepository conversaRepository;
    private final MensagemDaConversaRepository mensagemRepository;
    private final MinioUploadService minioUploadService;

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
     * Adiciona uma mensagem de texto a uma conversa.
     */
    @Transactional
    public MensagemDaConversaDto adicionarMensagemTexto(UUID conversaId, String texto, Instant recebidaEm, boolean enviada) {
        Conversa conversa = findOrThrow(conversaId);
        MensagemDaConversa msg = MensagemDaConversa.builder()
                .conversa(conversa)
                .texto(texto)
                .recebidaEm(recebidaEm != null ? recebidaEm : Instant.now())
                .enviada(enviada)
                .build();
        return MensagemDaConversaDto.from(mensagemRepository.save(msg));
    }

    /**
     * Adiciona uma mensagem com arquivo (imagem, áudio, vídeo, documento).
     * O arquivo é enviado ao MinIO e a URL é persistida.
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

        MensagemDaConversa msg = MensagemDaConversa.builder()
                .conversa(conversa)
                .texto(texto)
                .recebidaEm(recebidaEm != null ? recebidaEm : Instant.now())
                .urlArquivo(urlArquivo)
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

    private String obterExtensao(String nomeArquivo) {
        if (nomeArquivo != null && nomeArquivo.contains(".")) {
            return nomeArquivo.substring(nomeArquivo.lastIndexOf('.') + 1);
        }
        return "bin";
    }

    private java.io.InputStream getInputStream(MultipartFile arquivo) {
        try {
            return arquivo.getInputStream();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Erro ao ler arquivo enviado", e);
        }
    }
}
