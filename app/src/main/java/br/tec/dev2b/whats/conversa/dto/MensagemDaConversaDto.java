package br.tec.dev2b.whats.conversa.dto;

import br.tec.dev2b.whats.conversa.model.MensagemDaConversa;

import java.time.Instant;
import java.util.UUID;

public record MensagemDaConversaDto(
        UUID id,
        UUID conversaId,
        String texto,
        Instant recebidaEm,
        String urlArquivo,
        boolean enviada
) {
    public static MensagemDaConversaDto from(MensagemDaConversa m) {
        return new MensagemDaConversaDto(
                m.getId(),
                m.getConversa().getId(),
                m.getTexto(),
                m.getRecebidaEm(),
                m.getUrlArquivo(),
                m.isEnviada()
        );
    }
}
