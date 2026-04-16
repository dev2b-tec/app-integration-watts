package br.tec.dev2b.whats.chatbot.service;

import br.tec.dev2b.whats.chatbot.dto.ChatbotFluxoDto;
import br.tec.dev2b.whats.chatbot.dto.SalvarChatbotFluxoDto;
import br.tec.dev2b.whats.chatbot.model.ChatbotFluxo;
import br.tec.dev2b.whats.chatbot.repository.ChatbotFluxoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatbotFluxoService {

    private final ChatbotFluxoRepository repository;

    public List<ChatbotFluxoDto> listarPorEmpresa(UUID empresaId) {
        return repository.findAllByEmpresaId(empresaId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public ChatbotFluxoDto buscarPorId(UUID empresaId, UUID id) {
        return repository.findByIdAndEmpresaId(id, empresaId)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fluxo não encontrado"));
    }

    @Transactional
    public ChatbotFluxoDto criar(UUID empresaId, SalvarChatbotFluxoDto dto) {
        ChatbotFluxo fluxo = new ChatbotFluxo();
        fluxo.setEmpresaId(empresaId);
        fluxo.setNome(dto.nome());
        fluxo.setDescricao(dto.descricao());
        fluxo.setAtivo(Boolean.TRUE.equals(dto.ativo()));
        fluxo.setFluxo(dto.fluxo());
        return toDto(repository.save(fluxo));
    }

    @Transactional
    public ChatbotFluxoDto atualizar(UUID empresaId, UUID id, SalvarChatbotFluxoDto dto) {
        ChatbotFluxo fluxo = repository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fluxo não encontrado"));
        fluxo.setNome(dto.nome());
        fluxo.setDescricao(dto.descricao());
        fluxo.setAtivo(Boolean.TRUE.equals(dto.ativo()));
        fluxo.setFluxo(dto.fluxo());
        return toDto(repository.save(fluxo));
    }

    @Transactional
    public void excluir(UUID empresaId, UUID id) {
        if (repository.findByIdAndEmpresaId(id, empresaId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fluxo não encontrado");
        }
        repository.deleteByIdAndEmpresaId(id, empresaId);
    }

    private ChatbotFluxoDto toDto(ChatbotFluxo f) {
        return new ChatbotFluxoDto(
                f.getId(),
                f.getEmpresaId(),
                f.getNome(),
                f.getDescricao(),
                f.isAtivo(),
                f.getFluxo(),
                f.getCriadoEm(),
                f.getAtualizadoEm()
        );
    }
}
