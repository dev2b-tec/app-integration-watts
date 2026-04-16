package br.tec.dev2b.whats.chatbot.controller;

import br.tec.dev2b.whats.chatbot.dto.ChatbotFluxoDto;
import br.tec.dev2b.whats.chatbot.dto.SalvarChatbotFluxoDto;
import br.tec.dev2b.whats.chatbot.service.ChatbotFluxoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chatbot/empresa/{empresaId}/fluxos")
@RequiredArgsConstructor
public class ChatbotFluxoController {

    private final ChatbotFluxoService service;

    @GetMapping
    public List<ChatbotFluxoDto> listar(@PathVariable UUID empresaId) {
        return service.listarPorEmpresa(empresaId);
    }

    @GetMapping("/{id}")
    public ChatbotFluxoDto buscar(@PathVariable UUID empresaId, @PathVariable UUID id) {
        return service.buscarPorId(empresaId, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChatbotFluxoDto criar(@PathVariable UUID empresaId,
                                  @RequestBody @Valid SalvarChatbotFluxoDto dto) {
        return service.criar(empresaId, dto);
    }

    @PutMapping("/{id}")
    public ChatbotFluxoDto atualizar(@PathVariable UUID empresaId,
                                      @PathVariable UUID id,
                                      @RequestBody @Valid SalvarChatbotFluxoDto dto) {
        return service.atualizar(empresaId, id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID empresaId, @PathVariable UUID id) {
        service.excluir(empresaId, id);
    }
}
