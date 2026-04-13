package br.tec.dev2b.whats.conversa.controller;

import br.tec.dev2b.whats.conversa.dto.*;
import br.tec.dev2b.whats.conversa.model.StatusConversa;
import br.tec.dev2b.whats.conversa.service.ConversaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversas")
@RequiredArgsConstructor
public class ConversaController {

    private final ConversaService conversaService;

    // ── Conversas ─────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ConversaDto> criar(@RequestBody CriarConversaDto dto) {
        return ResponseEntity.ok(conversaService.criar(dto));
    }

    /** Retorna conversa ativa para o telefone ou cria uma nova. */
    @PostMapping("/buscar-ou-criar")
    public ResponseEntity<ConversaDto> buscarOuCriar(
            @RequestParam UUID empresaId,
            @RequestParam String telefone,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) UUID clienteId) {
        return ResponseEntity.ok(conversaService.buscarOuCriar(empresaId, telefone, nome, clienteId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversaDto> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(conversaService.buscarPorId(id));
    }

    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<Page<ConversaDto>> listarPorEmpresa(
            @PathVariable UUID empresaId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(conversaService.listarPorEmpresa(empresaId, pageable));
    }

    @GetMapping("/empresa/{empresaId}/status/{status}")
    public ResponseEntity<List<ConversaDto>> listarPorStatus(
            @PathVariable UUID empresaId,
            @PathVariable StatusConversa status) {
        return ResponseEntity.ok(conversaService.listarPorEmpresaEStatus(empresaId, status));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ConversaDto> alterarStatus(
            @PathVariable UUID id,
            @RequestBody AlterarStatusConversaDto dto) {
        return ResponseEntity.ok(conversaService.alterarStatus(id, dto));
    }

    // ── Mensagens da conversa ─────────────────────────────────────────────────

    @PostMapping("/{id}/mensagens/texto")
    public ResponseEntity<MensagemDaConversaDto> adicionarTexto(
            @PathVariable UUID id,
            @RequestParam String texto,
            @RequestParam(required = false) Instant recebidaEm,
            @RequestParam(defaultValue = "true") boolean enviada) {
        return ResponseEntity.ok(conversaService.adicionarMensagemTexto(id, texto, recebidaEm, enviada));
    }

    @PostMapping("/{id}/mensagens/arquivo")
    public ResponseEntity<MensagemDaConversaDto> adicionarArquivo(
            @PathVariable UUID id,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Instant recebidaEm,
            @RequestPart("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(conversaService.adicionarMensagemComArquivo(id, texto, recebidaEm, arquivo));
    }

    @PostMapping("/{id}/mensagens/audio")
    public ResponseEntity<MensagemDaConversaDto> adicionarAudio(
            @PathVariable UUID id,
            @RequestPart("audio") MultipartFile audio) {
        return ResponseEntity.ok(conversaService.adicionarMensagemAudio(id, audio));
    }

    @PostMapping("/{id}/mensagens/localizacao")
    public ResponseEntity<MensagemDaConversaDto> adicionarLocalizacao(
            @PathVariable UUID id,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String endereco) {
        return ResponseEntity.ok(
                conversaService.adicionarMensagemLocalizacao(id, latitude, longitude, nome, endereco));
    }

    @GetMapping("/{id}/mensagens")
    public ResponseEntity<Page<MensagemDaConversaDto>> listarMensagens(
            @PathVariable UUID id,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(conversaService.listarMensagens(id, pageable));
    }
}
