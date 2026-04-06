package br.tec.dev2b.whats.instancia.service;

import br.tec.dev2b.whats.infra.evolution.EvolutionApiClient;
import br.tec.dev2b.whats.infra.evolution.dto.*;
import br.tec.dev2b.whats.infra.websocket.WebSocketNotificationService;
import br.tec.dev2b.whats.instancia.dto.CriarInstanciaDto;
import br.tec.dev2b.whats.instancia.dto.InstanciaDto;
import br.tec.dev2b.whats.instancia.model.Instancia;
import br.tec.dev2b.whats.instancia.repository.InstanciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InstanciaService {

    private final InstanciaRepository instanciaRepository;
    private final EvolutionApiClient evolutionApiClient;
    private final WebSocketNotificationService webSocketService;

    @Value("${app.webhook.base-url}")
    private String webhookBaseUrl;

    @Transactional
    public InstanciaDto criar(CriarInstanciaDto dto) {
        if (instanciaRepository.existsByEmpresaId(dto.getEmpresaId())) {
            throw new IllegalArgumentException("Esta empresa já possui uma instância WhatsApp configurada.");
        }

        String instanceName = resolverInstanceName(dto);

        if (instanciaRepository.existsByInstanceName(instanceName)) {
            throw new IllegalArgumentException("instanceName já em uso: " + instanceName);
        }

        // Registra na Evolution API
        CriarInstanciaApiRequest apiRequest = new CriarInstanciaApiRequest();
        apiRequest.setInstanceName(instanceName);

        CriarInstanciaApiResponse apiResponse = evolutionApiClient.criarInstancia(apiRequest);

        String qrCode = null;
        if (apiResponse.getQrcode() != null) {
            qrCode = apiResponse.getQrcode().getBase64();
        }

        Instancia instancia = Instancia.builder()
                .empresaId(dto.getEmpresaId())
                .usuarioId(dto.getUsuarioId())
                .nome(dto.getNome())
                .instanceName(instanceName)
                .status("CONECTANDO")
                .qrCode(qrCode)
                .build();

        instancia = instanciaRepository.save(instancia);

        // Configura webhook automaticamente após criar
        configurarWebhookInterno(instancia);

        InstanciaDto result = InstanciaDto.from(instancia);
        webSocketService.notificarStatusInstancia(instancia.getEmpresaId(), result);
        return result;
    }

    @Transactional(readOnly = true)
    public List<InstanciaDto> listarPorEmpresa(UUID empresaId) {
        return instanciaRepository.findByEmpresaId(empresaId)
                .stream().map(InstanciaDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InstanciaDto> listarPorUsuario(UUID usuarioId) {
        return instanciaRepository.findByUsuarioId(usuarioId)
                .stream().map(InstanciaDto::from).toList();
    }

    @Transactional(readOnly = true)
    public InstanciaDto buscarPorId(UUID id) {
        return instanciaRepository.findById(id)
                .map(InstanciaDto::from)
                .orElseThrow(() -> new IllegalArgumentException("Instância não encontrada: " + id));
    }

    @Transactional
    public InstanciaDto reconectar(UUID id) {
        Instancia instancia = instanciaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Instância não encontrada: " + id));

        ConectarInstanciaResponse response = evolutionApiClient.conectarInstancia(instancia.getInstanceName());

        if (response != null && response.getBase64() != null) {
            instancia.setQrCode(response.getBase64());
            instancia.setStatus("CONECTANDO");
            instancia = instanciaRepository.save(instancia);

            webSocketService.notificarQrCode(instancia.getId(), response.getBase64());
        }

        InstanciaDto result = InstanciaDto.from(instancia);
        webSocketService.notificarStatusInstancia(instancia.getEmpresaId(), result);
        return result;
    }

    @Transactional
    public void excluir(UUID id) {
        Instancia instancia = instanciaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Instância não encontrada: " + id));

        try {
            evolutionApiClient.excluirInstancia(instancia.getInstanceName());
        } catch (Exception ignored) {
            // Continua exclusão local mesmo se a API retornar erro
        }

        instanciaRepository.delete(instancia);
    }

    @Transactional
    public InstanciaDto configurarWebhook(UUID id) {
        Instancia instancia = instanciaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Instância não encontrada: " + id));

        configurarWebhookInterno(instancia);
        return InstanciaDto.from(instanciaRepository.save(instancia));
    }

    /** Chamado internamente após criar ou manualmente via endpoint */
    private void configurarWebhookInterno(Instancia instancia) {
        ConfigurarWebhookRequest req = new ConfigurarWebhookRequest();
        req.getWebhook().setUrl(webhookBaseUrl + "/api/v1/webhook/evolution");

        evolutionApiClient.configurarWebhook(instancia.getInstanceName(), req);
        instancia.setWebhookConfigurado(true);
    }

    /** Atualiza status via evento do webhook (chamado pelo WebhookController) */
    @Transactional
    public void atualizarStatusPorInstanceName(String instanceName, String novoStatus, String numero, String qrCode) {
        instanciaRepository.findByInstanceName(instanceName).ifPresent(instancia -> {
            instancia.setStatus(novoStatus);
            if (numero != null) instancia.setNumero(numero);
            if (qrCode != null) instancia.setQrCode(qrCode);
            instanciaRepository.save(instancia);

            InstanciaDto dto = InstanciaDto.from(instancia);
            webSocketService.notificarStatusInstancia(instancia.getEmpresaId(), dto);

            if (qrCode != null) {
                webSocketService.notificarQrCode(instancia.getId(), qrCode);
            }
        });
    }

    // ------------------------------------------------

    private String resolverInstanceName(CriarInstanciaDto dto) {
        // Identificador padrão = empresaId (único e estável)
        if (dto.getInstanceName() != null && !dto.getInstanceName().isBlank()) {
            return dto.getInstanceName().trim().toLowerCase().replaceAll("[^a-z0-9\\-_]", "-");
        }
        return "empresa-" + dto.getEmpresaId().toString();
    }
}
