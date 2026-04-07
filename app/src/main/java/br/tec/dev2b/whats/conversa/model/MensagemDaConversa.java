package br.tec.dev2b.whats.conversa.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "mensagens_da_conversa",
    indexes = {
        @Index(name = "idx_msgconv_conversa_id", columnList = "conversa_id"),
        @Index(name = "idx_msgconv_recebida_em", columnList = "recebida_em")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensagemDaConversa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversa_id", nullable = false)
    private Conversa conversa;

    /** Texto da mensagem (nulo quando for mensagem de mídia apenas). */
    @Column(columnDefinition = "TEXT")
    private String texto;

    /**
     * Timestamp de recebimento com precisão de milissegundos
     * (armazenado como TIMESTAMPTZ no PostgreSQL).
     */
    @Column(name = "recebida_em", nullable = false)
    private Instant recebidaEm;

    /**
     * URL do arquivo no MinIO — preenchido quando a mensagem
     * contiver imagem, áudio, vídeo ou documento.
     */
    @Column(name = "url_arquivo", columnDefinition = "TEXT")
    private String urlArquivo;

    /**
     * {@code true} = mensagem enviada por nós (fromMe);
     * {@code false} = mensagem recebida do contato.
     */
    @Column(name = "enviada", nullable = false)
    private boolean enviada;
}
