package br.tec.dev2b.whats.infra.evolution;

import br.tec.dev2b.whats.infra.evolution.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class EvolutionApiClient {

    private final RestClient evolutionRestClient;

    public CriarInstanciaApiResponse criarInstancia(CriarInstanciaApiRequest request) {
        return evolutionRestClient.post()
                .uri("/instance/create")
                .body(request)
                .retrieve()
                .body(CriarInstanciaApiResponse.class);
    }

    public ConectarInstanciaResponse conectarInstancia(String instanceName) {
        return evolutionRestClient.get()
                .uri("/instance/connect/{name}", instanceName)
                .retrieve()
                .body(ConectarInstanciaResponse.class);
    }

    public void excluirInstancia(String instanceName) {
        evolutionRestClient.delete()
                .uri("/instance/delete/{name}", instanceName)
                .retrieve()
                .toBodilessEntity();
    }

    public void configurarWebhook(String instanceName, ConfigurarWebhookRequest request) {
        evolutionRestClient.post()
                .uri("/webhook/set/{name}", instanceName)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public void enviarTexto(String instanceName, EnviarTextoRequest request) {
        evolutionRestClient.post()
                .uri("/message/sendText/{name}", instanceName)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
