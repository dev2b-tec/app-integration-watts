package br.tec.dev2b.whats.infra.evolution.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/** POST /message/sendLocation/{instance} */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnviarLocalizacaoRequest {
    private String number;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private Integer delay;
}
