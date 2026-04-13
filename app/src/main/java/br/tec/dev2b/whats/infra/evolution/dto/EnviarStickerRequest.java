package br.tec.dev2b.whats.infra.evolution.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/** POST /message/sendSticker/{instance} */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnviarStickerRequest {
    private String number;
    /** URL ou Base64 do sticker (WebP) */
    private String sticker;
    private Integer delay;
}
