package br.tec.dev2b.whats.conversa.dto;

import br.tec.dev2b.whats.conversa.model.Conversa;
import br.tec.dev2b.whats.conversa.model.StatusConversa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ConversaDto(
        UUID id,
        UUID empresaId,
        String telefone,
        String nome,
        UUID clienteId,
        StatusConversa status,
        LocalDateTime createdAt,
        List<MensagemDaConversaDto> mensagens
) {
    public static ConversaDto from(Conversa c) {
        return new ConversaDto(
                c.getId(),
                c.getEmpresaId(),
                c.getTelefone(),
                c.getNome(),
                c.getClienteId(),
                c.getStatus(),
                c.getCreatedAt(),
                c.getMensagens().stream().map(MensagemDaConversaDto::from).toList()
        );
    }

    public static ConversaDto resumo(Conversa c) {
        return new ConversaDto(
                c.getId(),
                c.getEmpresaId(),
                c.getTelefone(),
                c.getNome(),
                c.getClienteId(),
                c.getStatus(),
                c.getCreatedAt(),
                List.of()
        );
    }
}
