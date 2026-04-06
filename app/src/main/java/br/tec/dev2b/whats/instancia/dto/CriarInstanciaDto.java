package br.tec.dev2b.whats.instancia.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CriarInstanciaDto {
    private UUID empresaId;
    private UUID usuarioId;
    private String nome;
    /**
     * instanceName é o identificador único no Evolution API.
     * Se não informado, será gerado automaticamente a partir do nome.
     */
    private String instanceName;
}
