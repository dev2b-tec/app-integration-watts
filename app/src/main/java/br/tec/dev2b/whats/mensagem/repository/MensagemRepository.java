package br.tec.dev2b.whats.mensagem.repository;

import br.tec.dev2b.whats.mensagem.model.Mensagem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MensagemRepository extends JpaRepository<Mensagem, UUID> {

    Page<Mensagem> findByInstanciaIdOrderByCreatedAtDesc(UUID instanciaId, Pageable pageable);

    List<Mensagem> findByInstanciaIdAndRemoteJidOrderByCreatedAtAsc(UUID instanciaId, String remoteJid);
}
