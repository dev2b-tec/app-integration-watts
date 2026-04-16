package br.tec.dev2b.whats.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SalvarChatbotFluxoDto(
        @NotBlank @Size(max = 100) String nome,
        String descricao,
        Boolean ativo,
        @NotNull FluxoData fluxo
) {}
