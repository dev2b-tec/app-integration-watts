package br.tec.dev2b.whats.infra.evolution.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CriarInstanciaApiResponse {
    private InstanceInfo instance;
    private HashInfo hash;
    private QrCodeInfo qrcode;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InstanceInfo {
        private String instanceName;
        private String status;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HashInfo {
        private String apikey;

        @JsonCreator
        public HashInfo(String value) {
            this.apikey = value;
        }

        public HashInfo() {}
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QrCodeInfo {
        private String base64;
        private String code;
    }
}
