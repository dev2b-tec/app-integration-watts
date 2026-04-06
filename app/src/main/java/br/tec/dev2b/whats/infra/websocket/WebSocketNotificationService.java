package br.tec.dev2b.whats.infra.websocket;

import br.tec.dev2b.whats.instancia.dto.InstanciaDto;
import br.tec.dev2b.whats.mensagem.dto.MensagemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Envia eventos em tempo real ao front-end via STOMP/WebSocket.
 *
 * Tópicos:
 *   /topic/instancias/{empresaId}/status   → mudança de status da instância
 *   /topic/instancias/{instanciaId}/qrcode → novo QR Code disponível
 *   /topic/instancias/{instanciaId}/mensagens → nova mensagem recebida/enviada
 */
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notificarStatusInstancia(UUID empresaId, InstanciaDto instancia) {
        messagingTemplate.convertAndSend(
                "/topic/instancias/" + empresaId + "/status",
                instancia
        );
    }

    public void notificarQrCode(UUID instanciaId, String qrCodeBase64) {
        messagingTemplate.convertAndSend(
                "/topic/instancias/" + instanciaId + "/qrcode",
                qrCodeBase64
        );
    }

    public void notificarMensagem(UUID instanciaId, MensagemDto mensagem) {
        messagingTemplate.convertAndSend(
                "/topic/instancias/" + instanciaId + "/mensagens",
                mensagem
        );
    }
}
