package br.tec.dev2b.whats.instancia.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "instancias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Instancia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(nullable = false)
    private String nome;

    @Column(name = "instance_name", nullable = false, unique = true)
    private String instanceName;

    @Column(nullable = false)
    @Builder.Default
    private String status = "CRIADA";

    @Column(length = 30)
    private String numero;

    @Column(columnDefinition = "TEXT")
    private String qrCode;

    @Column(name = "webhook_configurado", nullable = false)
    @Builder.Default
    private Boolean webhookConfigurado = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
