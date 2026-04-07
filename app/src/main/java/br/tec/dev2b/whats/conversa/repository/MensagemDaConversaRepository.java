package br.tec.dev2b.whats.conversa.repository;

import br.tec.dev2b.whats.conversa.model.MensagemDaConversa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MensagemDaConversaRepository extends JpaRepository<MensagemDaConversa, UUID> {

    Page<MensagemDaConversa> findByConversaIdOrderByRecebidaEmAsc(UUID conversaId, Pageable pageable);
}
