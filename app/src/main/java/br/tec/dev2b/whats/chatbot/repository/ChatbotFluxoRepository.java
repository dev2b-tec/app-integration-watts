package br.tec.dev2b.whats.chatbot.repository;

import br.tec.dev2b.whats.chatbot.model.ChatbotFluxo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatbotFluxoRepository extends JpaRepository<ChatbotFluxo, UUID> {

    List<ChatbotFluxo> findAllByEmpresaId(UUID empresaId);

    Optional<ChatbotFluxo> findByIdAndEmpresaId(UUID id, UUID empresaId);

    void deleteByIdAndEmpresaId(UUID id, UUID empresaId);
}
