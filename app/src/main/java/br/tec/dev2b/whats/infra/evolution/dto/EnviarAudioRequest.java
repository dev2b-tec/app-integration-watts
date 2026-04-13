package br.tec.dev2b.whats.infra.evolution.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/** POST /message/sendWhatsAppAudio/{instance} */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnviarAudioRequest {
    private String number;
    /** URL ou Base64 do arquivo de áudio */
    private String audio;
    private Integer delay;
}
