package br.tec.dev2b.whats.mensagem.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class EnviarMensagemDto {
    private UUID instanciaId;
    /** Número no formato internacional: 5581999990000 */
    private String numero;
    private String texto;
}
