package br.tec.dev2b.whats.instancia.dto;

import br.tec.dev2b.whats.instancia.model.Instancia;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class InstanciaDto {
    private UUID id;
    private UUID empresaId;
    private UUID usuarioId;
    private String nome;
    private String instanceName;
    private String status;
    private String numero;
    private String qrCode;
    private Boolean webhookConfigurado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static InstanciaDto from(Instancia i) {
        InstanciaDto dto = new InstanciaDto();
        dto.id = i.getId();
        dto.empresaId = i.getEmpresaId();
        dto.usuarioId = i.getUsuarioId();
        dto.nome = i.getNome();
        dto.instanceName = i.getInstanceName();
        dto.status = i.getStatus();
        dto.numero = i.getNumero();
        dto.qrCode = i.getQrCode();
        dto.webhookConfigurado = i.getWebhookConfigurado();
        dto.createdAt = i.getCreatedAt();
        dto.updatedAt = i.getUpdatedAt();
        return dto;
    }
}
