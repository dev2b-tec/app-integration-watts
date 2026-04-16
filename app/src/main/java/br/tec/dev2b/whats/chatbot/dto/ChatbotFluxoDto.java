package br.tec.dev2b.whats.chatbot.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatbotFluxoDto(
        UUID id,
        UUID empresaId,
        String nome,
        String descricao,
        boolean ativo,
        FluxoData fluxo,
        Instant criadoEm,
        Instant atualizadoEm
) {}
