package br.tec.dev2b.whats.mensagem.model;

import br.tec.dev2b.whats.instancia.model.Instancia;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mensagens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instancia_id", nullable = false)
    private Instancia instancia;

    @Column(name = "remote_jid", nullable = false, length = 100)
    private String remoteJid;

    @Column(length = 30)
    private String numero;

    @Column(name = "push_name")
    private String pushName;

    @Column(name = "message_id")
    private String messageId;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String tipo = "TEXTO";

    @Column(columnDefinition = "TEXT")
    private String conteudo;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String direcao = "RECEBIDA";

    @Column(name = "status_envio", nullable = false, length = 20)
    @Builder.Default
    private String statusEnvio = "PENDENTE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
