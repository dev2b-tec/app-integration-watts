package br.tec.dev2b.whats.chatbot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import br.tec.dev2b.whats.chatbot.dto.FluxoData;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "chatbot_fluxos")
public class ChatbotFluxo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column
    private String descricao;

    @Column(nullable = false)
    private boolean ativo = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private FluxoData fluxo;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    void prePersist() {
        criadoEm = Instant.now();
        atualizadoEm = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = Instant.now();
    }
}
