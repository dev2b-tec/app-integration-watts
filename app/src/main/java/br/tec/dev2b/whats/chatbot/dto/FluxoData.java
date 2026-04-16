package br.tec.dev2b.whats.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FluxoData(
        List<FluxoNo> nodes,
        List<FluxoAresta> edges
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FluxoNo(
            String id,
            String type,
            FluxoPosicao position,
            java.util.Map<String, Object> data
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FluxoPosicao(double x, double y) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FluxoAresta(
            String id,
            String source,
            String target,
            String sourceHandle,
            String targetHandle,
            String label
    ) {}
}
