package br.tec.dev2b.whats.infra.evolution.dto;

import lombok.Data;

import java.util.List;

@Data
public class ConfigurarWebhookRequest {
    private WebhookConfig webhook = new WebhookConfig();

    @Data
    public static class WebhookConfig {
        private String url;
        private boolean enabled = true;
        private boolean webhookByEvents = true;
        private List<String> events = List.of(
                "MESSAGES_UPSERT",
                "MESSAGES_UPDATE",
                "CONNECTION_UPDATE",
                "QRCODE_UPDATED"
        );
    }
}
