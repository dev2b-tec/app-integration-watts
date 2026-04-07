package br.tec.dev2b.whats.conversa.dto;

import java.util.UUID;

public record CriarConversaDto(
        UUID empresaId,
        String telefone,
        String nome,
        UUID clienteId
) {}
