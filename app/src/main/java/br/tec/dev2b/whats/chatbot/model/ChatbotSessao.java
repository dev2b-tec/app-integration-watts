package br.tec.dev2b.whats.chatbot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "chatbot_sessoes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"instance_name", "telefone"}))
public class ChatbotSessao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "instance_name", nullable = false, length = 100)
    private String instanceName;

    @Column(nullable = false, length = 20)
    private String telefone;

    @Column(name = "fluxo_id", nullable = false)
    private UUID fluxoId;

    @Column(name = "no_atual_id", nullable = false, length = 100)
    private String noAtualId;

    @Column(name = "aguardando_input", nullable = false)
    private boolean aguardandoInput = false;

    /** Variáveis da conversa — ex: {"nome_paciente": "Jessé"} */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, String> variaveis = new HashMap<>();

    @Column(name = "iniciado_em", nullable = false)
    private Instant iniciadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    void prePersist() {
        iniciadoEm = Instant.now();
        atualizadoEm = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = Instant.now();
    }
}
