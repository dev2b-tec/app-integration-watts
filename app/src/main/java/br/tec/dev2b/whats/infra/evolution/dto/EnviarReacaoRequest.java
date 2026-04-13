package br.tec.dev2b.whats.infra.evolution.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/** POST /message/sendReaction/{instance} */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnviarReacaoRequest {

    private Key key;
    /** Emoji da reação, ex: "🚀" */
    private String reaction;

    @Data
    public static class Key {
        private String remoteJid;
        private Boolean fromMe;
        private String id;
    }
}
