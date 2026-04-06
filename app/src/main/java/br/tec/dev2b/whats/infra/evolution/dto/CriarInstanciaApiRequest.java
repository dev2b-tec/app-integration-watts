package br.tec.dev2b.whats.infra.evolution.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CriarInstanciaApiRequest {
    private String instanceName;
    @JsonProperty("qrcode")
    private boolean qrcode = true;
    private String integration = "WHATSAPP-BAILEYS";
}
