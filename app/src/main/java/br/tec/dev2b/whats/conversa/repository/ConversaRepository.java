package br.tec.dev2b.whats.conversa.repository;

import br.tec.dev2b.whats.conversa.model.Conversa;
import br.tec.dev2b.whats.conversa.model.StatusConversa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversaRepository extends JpaRepository<Conversa, UUID> {

    Page<Conversa> findByEmpresaIdOrderByCreatedAtDesc(UUID empresaId, Pageable pageable);

    List<Conversa> findByEmpresaIdAndStatus(UUID empresaId, StatusConversa status);

    Optional<Conversa> findByEmpresaIdAndTelefoneAndStatusNot(
            UUID empresaId, String telefone, StatusConversa statusExcluido);

    /** Busca conversa ativa (ATIVA ou EM_ATENDIMENTO) para um telefone numa empresa. */
    Optional<Conversa> findByEmpresaIdAndTelefoneAndStatusIn(
            UUID empresaId, String telefone, List<StatusConversa> statuses);

    /** Verifica se já existe conversa com atendimento humano (ATIVA ou EM_ATENDIMENTO) para o telefone. */
    boolean existsByEmpresaIdAndTelefoneAndStatusIn(
            UUID empresaId, String telefone, List<StatusConversa> statuses);

    /** Conversas com determinados status criadas antes de um instante (para expiração). */
    List<Conversa> findByStatusInAndCreatedAtBefore(
            List<StatusConversa> statuses, LocalDateTime before);
}
