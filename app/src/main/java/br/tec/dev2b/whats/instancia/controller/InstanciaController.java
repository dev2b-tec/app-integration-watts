package br.tec.dev2b.whats.instancia.controller;

import br.tec.dev2b.whats.instancia.dto.CriarInstanciaDto;
import br.tec.dev2b.whats.instancia.dto.InstanciaDto;
import br.tec.dev2b.whats.instancia.service.InstanciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/instancias")
@RequiredArgsConstructor
public class InstanciaController {

    private final InstanciaService instanciaService;

    @PostMapping
    public ResponseEntity<InstanciaDto> criar(@RequestBody CriarInstanciaDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(instanciaService.criar(dto));
    }

    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<List<InstanciaDto>> listarPorEmpresa(@PathVariable UUID empresaId) {
        return ResponseEntity.ok(instanciaService.listarPorEmpresa(empresaId));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<InstanciaDto>> listarPorUsuario(@PathVariable UUID usuarioId) {
        return ResponseEntity.ok(instanciaService.listarPorUsuario(usuarioId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InstanciaDto> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(instanciaService.buscarPorId(id));
    }

    /** Reconecta instância e retorna novo QR Code (se necessário) */
    @PostMapping("/{id}/reconectar")
    public ResponseEntity<InstanciaDto> reconectar(@PathVariable UUID id) {
        return ResponseEntity.ok(instanciaService.reconectar(id));
    }

    /** Reconfigura webhook na Evolution API */
    @PostMapping("/{id}/webhook")
    public ResponseEntity<InstanciaDto> configurarWebhook(@PathVariable UUID id) {
        return ResponseEntity.ok(instanciaService.configurarWebhook(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        instanciaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
