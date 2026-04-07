package br.tec.dev2b.whats.conversa.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "conversas",
    indexes = {
        @Index(name = "idx_conversas_empresa_id", columnList = "empresa_id"),
        @Index(name = "idx_conversas_telefone",   columnList = "telefone"),
        @Index(name = "idx_conversas_status",      columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(nullable = false, length = 30)
    private String telefone;

    /** Nome/apelido da pessoa com quem estamos conversando (ex: pushName do WhatsApp). */
    @Column(length = 150)
    private String nome;

    /** ID do cliente cadastrado no sistema, quando conhecido. */
    @Column(name = "cliente_id")
    private UUID clienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatusConversa status = StatusConversa.ATIVA;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "conversa", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("recebidaEm ASC")
    @Builder.Default
    private List<MensagemDaConversa> mensagens = new ArrayList<>();

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
