package br.tec.dev2b.whats.infra.evolution.dto;

import lombok.Data;

@Data
public class EnviarTextoRequest {
    private String number;
    private String text;
}
