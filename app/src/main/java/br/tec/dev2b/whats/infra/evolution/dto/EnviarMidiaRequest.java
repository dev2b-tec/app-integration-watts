package br.tec.dev2b.whats.infra.evolution.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/** POST /message/sendMedia/{instance} */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnviarMidiaRequest {
    private String number;
    /** "image" | "video" | "document" */
    private String mediatype;
    private String mimetype;
    private String caption;
    /** URL ou Base64 */
    private String media;
    private String fileName;
    private Integer delay;
}
