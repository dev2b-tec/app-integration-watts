package br.tec.dev2b.whats.mensagem.dto;

import br.tec.dev2b.whats.mensagem.model.Mensagem;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MensagemDto {
    private UUID id;
    private UUID instanciaId;
    private String remoteJid;
    private String numero;
    private String pushName;
    private String messageId;
    private String tipo;
    private String conteudo;
    private String direcao;
    private String statusEnvio;
    private LocalDateTime createdAt;

    public static MensagemDto from(Mensagem m) {
        MensagemDto dto = new MensagemDto();
        dto.id = m.getId();
        dto.instanciaId = m.getInstancia().getId();
        dto.remoteJid = m.getRemoteJid();
        dto.numero = m.getNumero();
        dto.pushName = m.getPushName();
        dto.messageId = m.getMessageId();
        dto.tipo = m.getTipo();
        dto.conteudo = m.getConteudo();
        dto.direcao = m.getDirecao();
        dto.statusEnvio = m.getStatusEnvio();
        dto.createdAt = m.getCreatedAt();
        return dto;
    }
}
