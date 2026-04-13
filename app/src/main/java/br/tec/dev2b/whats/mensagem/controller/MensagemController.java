package br.tec.dev2b.whats.mensagem.controller;

import br.tec.dev2b.whats.mensagem.dto.EnviarMensagemDto;
import br.tec.dev2b.whats.mensagem.dto.MensagemDto;
import br.tec.dev2b.whats.mensagem.service.MensagemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mensagens")
@RequiredArgsConstructor
public class MensagemController {

    private final MensagemService mensagemService;

    @PostMapping("/enviar")
    public ResponseEntity<MensagemDto> enviarTexto(@RequestBody EnviarMensagemDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mensagemService.enviarTexto(dto));
    }

    @PostMapping("/enviar/midia")
    public ResponseEntity<MensagemDto> enviarMidia(@RequestBody EnviarMensagemDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mensagemService.enviarMidia(dto));
    }

    @PostMapping("/enviar/audio")
    public ResponseEntity<MensagemDto> enviarAudio(@RequestBody EnviarMensagemDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mensagemService.enviarAudio(dto));
    }

    @PostMapping("/enviar/sticker")
    public ResponseEntity<MensagemDto> enviarSticker(@RequestBody EnviarMensagemDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mensagemService.enviarSticker(dto));
    }

    @PostMapping("/enviar/localizacao")
    public ResponseEntity<MensagemDto> enviarLocalizacao(@RequestBody EnviarMensagemDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mensagemService.enviarLocalizacao(dto));
    }

    @PostMapping("/enviar/reacao")
    public ResponseEntity<Void> enviarReacao(@RequestBody EnviarMensagemDto dto) {
        mensagemService.enviarReacao(dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/instancia/{instanciaId}")
    public ResponseEntity<Page<MensagemDto>> listar(
            @PathVariable UUID instanciaId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(mensagemService.listar(instanciaId, pageable));
    }

    @GetMapping("/instancia/{instanciaId}/conversa")
    public ResponseEntity<List<MensagemDto>> listarConversa(
            @PathVariable UUID instanciaId,
            @RequestParam String remoteJid) {
        return ResponseEntity.ok(mensagemService.listarConversa(instanciaId, remoteJid));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MensagemDto> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(mensagemService.buscarPorId(id));
    }
}
