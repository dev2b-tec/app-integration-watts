package br.tec.dev2b.whats.chatbot.repository;

import br.tec.dev2b.whats.chatbot.model.ChatbotSessao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChatbotSessaoRepository extends JpaRepository<ChatbotSessao, UUID> {

    Optional<ChatbotSessao> findByInstanceNameAndTelefone(String instanceName, String telefone);

    void deleteByInstanceNameAndTelefone(String instanceName, String telefone);
}
