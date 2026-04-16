package br.tec.dev2b.whats.chatbot.service;

import br.tec.dev2b.whats.chatbot.dto.FluxoData;
import br.tec.dev2b.whats.chatbot.dto.FluxoData.FluxoAresta;
import br.tec.dev2b.whats.chatbot.dto.FluxoData.FluxoNo;
import br.tec.dev2b.whats.chatbot.model.ChatbotFluxo;
import br.tec.dev2b.whats.chatbot.model.ChatbotSessao;
import br.tec.dev2b.whats.chatbot.repository.ChatbotFluxoRepository;
import br.tec.dev2b.whats.chatbot.repository.ChatbotSessaoRepository;
import br.tec.dev2b.whats.conversa.model.Conversa;
import br.tec.dev2b.whats.conversa.model.MensagemDaConversa;
import br.tec.dev2b.whats.conversa.model.StatusConversa;
import br.tec.dev2b.whats.conversa.repository.ConversaRepository;
import br.tec.dev2b.whats.conversa.repository.MensagemDaConversaRepository;
import br.tec.dev2b.whats.infra.evolution.EvolutionApiClient;
import br.tec.dev2b.whats.infra.evolution.dto.EnviarLocalizacaoRequest;
import br.tec.dev2b.whats.infra.evolution.dto.EnviarMidiaRequest;
import br.tec.dev2b.whats.infra.evolution.dto.EnviarTextoRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Motor de execução do chatbot.
 *
 * Fluxo principal:
 *  1. Mensagem recebida  → processar(instanceName, empresaId, telefone, input)
 *  2. Se há sessão ativa → retoma no nó atual (aguardando_input=true)
 *  3. Se não há sessão  → busca fluxo ativo da empresa → inicia no nó "inicio"
 *  4. Executa nós sequencialmente até encontrar um nó de input, fim ou "ir_para"
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotMotorService {

    private static final List<StatusConversa> STATUS_HUMANO =
            List.of(StatusConversa.ATIVA, StatusConversa.EM_ATENDIMENTO);

    private final ChatbotFluxoRepository fluxoRepository;
    private final ChatbotSessaoRepository sessaoRepository;
    private final ConversaRepository conversaRepository;
    private final MensagemDaConversaRepository mensagemDaConversaRepository;
    private final EvolutionApiClient evolutionApiClient;

    // ─────────────────────────────────────────────────────────────────────────
    // Ponto de entrada
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void processar(String instanceName, UUID empresaId, String telefone,
                           String inputUsuario, String pushName) {

        // ── Regra 1: se há atendimento humano ativo, chatbot não interfere ─────
        if (conversaRepository.existsByEmpresaIdAndTelefoneAndStatusIn(
                empresaId, telefone, STATUS_HUMANO)) {
            log.debug("[MOTOR] Conversa humana ativa para telefone={}, chatbot ignorado", telefone);
            return;
        }

        Optional<ChatbotSessao> sessaoOpt =
                sessaoRepository.findByInstanceNameAndTelefone(instanceName, telefone);

        if (sessaoOpt.isPresent()) {
            // Sessão ativa: retomar no nó que estava aguardando input
            ChatbotSessao sessao = sessaoOpt.get();
            Optional<ChatbotFluxo> fluxoOpt = fluxoRepository.findById(sessao.getFluxoId());
            if (fluxoOpt.isEmpty()) {
                encerrarSessao(sessao);
                return;
            }
            FluxoData fluxo = fluxoOpt.get().getFluxo();
            FluxoNo noAtual = encontrarNo(fluxo, sessao.getNoAtualId());
            if (noAtual == null) {
                encerrarSessao(sessao);
                return;
            }
            // Registra a mensagem do usuário na conversa EM_CHATBOT
            registrarMensagemNaConversa(empresaId, telefone, inputUsuario, false);
            // Continua a partir do nó atual processando o input recebido
            executarAPartirDo(instanceName, telefone, sessao, fluxo, noAtual, inputUsuario);

        } else {
            // ── Regra 2: sem sessão → sem conversa EM_CHATBOT aberta → inicia chatbot ─
            // Garante que não há conversa EM_CHATBOT já aberta para este telefone
            boolean jaEmChatbot = conversaRepository.existsByEmpresaIdAndTelefoneAndStatusIn(
                    empresaId, telefone, List.of(StatusConversa.EM_CHATBOT));
            if (jaEmChatbot) {
                // Estado inconsistente: conversa existe mas sessão não — limpa e reinicia
                conversaRepository.findByEmpresaIdAndTelefoneAndStatusIn(
                        empresaId, telefone, List.of(StatusConversa.EM_CHATBOT))
                        .ifPresent(c -> {
                            c.setStatus(StatusConversa.FINALIZADA);
                            conversaRepository.save(c);
                        });
            }

            // Busca fluxo ativo da empresa
            List<ChatbotFluxo> fluxosAtivos = fluxoRepository.findAllByEmpresaId(empresaId)
                    .stream().filter(ChatbotFluxo::isAtivo).toList();

            if (fluxosAtivos.isEmpty()) {
                log.debug("[MOTOR] Nenhum fluxo ativo para empresaId={}", empresaId);
                return;
            }

            ChatbotFluxo chatbotFluxo = fluxosAtivos.get(0);
            FluxoData fluxo = chatbotFluxo.getFluxo();
            FluxoNo noInicio = encontrarNoPorTipo(fluxo, "inicio");
            if (noInicio == null) {
                log.warn("[MOTOR] Fluxo {} sem nó 'inicio'", chatbotFluxo.getId());
                return;
            }

            // Cria conversa EM_CHATBOT
            Conversa conversa = Conversa.builder()
                    .empresaId(empresaId)
                    .telefone(telefone)
                    .nome(pushName)
                    .status(StatusConversa.EM_CHATBOT)
                    .build();
            conversaRepository.save(conversa);

            // Registra a mensagem inicial do usuário
            registrarMensagemNaConversa(empresaId, telefone, inputUsuario, false);

            ChatbotSessao sessao = criarSessao(instanceName, empresaId, telefone,
                    chatbotFluxo.getId(), noInicio.id());
            executarAPartirDo(instanceName, telefone, sessao, fluxo, noInicio, inputUsuario);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Execução sequencial de nós
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Executa nós em cadeia a partir de {@code noAtual}.
     * Para quando:
     *  - nó do tipo "fim"
     *  - nó que aguarda input do usuário (menu, entrada_dados, receber_anexo)
     *  - nenhuma aresta de saída
     *  - proteção contra loop (máx 50 nós por ciclo)
     */
    private void executarAPartirDo(String instanceName, String telefone,
                                    ChatbotSessao sessao, FluxoData fluxo,
                                    FluxoNo noAtual, String inputUsuario) {
        Set<String> visitados = new HashSet<>();
        FluxoNo no = noAtual;
        String proximoHandle = null; // handle de saída escolhido por menu/condição

        // Variáveis da sessão — mantidas em memória e persistidas a cada save
        Map<String, String> vars = sessao.getVariaveis() != null
                ? new HashMap<>(sessao.getVariaveis()) : new HashMap<>();

        int limite = 50;
        while (no != null && limite-- > 0) {
            if (!visitados.add(no.id())) {
                log.warn("[MOTOR] Loop detectado no nó {}, abortando", no.id());
                encerrarSessao(sessao);
                return;
            }

            String tipo = no.type();
            Map<String, Object> data = no.data() != null ? no.data() : Map.of();

            switch (tipo) {

                case "inicio" -> {
                    // Nó de início: apenas avança
                }

                case "mensagem" -> {
                    // campo no frontend: data.mensagem
                    String texto = interpolar(str(data, "mensagem"), vars);
                    if (texto != null && !texto.isBlank()) {
                        enviarTexto(instanceName, telefone, texto);
                    }
                }

                case "anexo" -> {
                    // campos no frontend: data.urlAnexo, data.tipoAnexo, data.legenda
                    String url     = str(data, "urlAnexo");
                    String tipoM   = str(data, "tipoAnexo"); // "imagem" | "video" | "documento"
                    String caption = str(data, "legenda");
                    String captionInterp = interpolar(caption, vars);
                    if (url != null && !url.isBlank()) {
                        String mediatype = switch (tipoM != null ? tipoM : "") {
                            case "video"     -> "video";
                            case "documento" -> "document";
                            default          -> "image";
                        };
                        EnviarMidiaRequest req = new EnviarMidiaRequest();
                        req.setNumber(telefone);
                        req.setMedia(url);
                        req.setMediatype(mediatype);
                        req.setCaption(captionInterp);
                        evolutionApiClient.enviarMidia(instanceName, req);
                    }
                }

                case "localizacao" -> {
                    Double lat      = num(data, "latitude");
                    Double lon      = num(data, "longitude");
                    String nome     = str(data, "nome");
                    String endereco = str(data, "endereco");
                    if (lat != null && lon != null) {
                        EnviarLocalizacaoRequest req = new EnviarLocalizacaoRequest();
                        req.setNumber(telefone);
                        req.setLatitude(lat);
                        req.setLongitude(lon);
                        req.setName(nome);
                        req.setAddress(endereco);
                        evolutionApiClient.enviarLocalizacao(instanceName, req);
                    }
                }

                case "menu" -> {
                    // campos no frontend: data.mensagem (texto), data.opcoes (string[])
                    // handles de saída: "op-0", "op-1", ...
                    String textoMenu = interpolar(str(data, "mensagem"), vars);
                    @SuppressWarnings("unchecked")
                    List<String> opcoes = data.get("opcoes") instanceof List<?> l
                            ? (List<String>) l : List.of();
                    List<String> opcoesInterp = opcoes.stream()
                            .map(op -> interpolar(op, vars)).collect(Collectors.toList());

                    if (sessao.isAguardandoInput()) {
                        // Usuário respondeu — resolve qual handle seguir
                        proximoHandle = resolverOpcaoMenu(opcoesInterp, inputUsuario != null ? inputUsuario.trim() : "");
                        sessao.setAguardandoInput(false);
                        no = proximoNo(fluxo, no.id(), proximoHandle);
                        proximoHandle = null;
                        continue;
                    } else {
                        // Primeira vez: envia menu e aguarda
                        StringBuilder sb = new StringBuilder();
                        if (textoMenu != null && !textoMenu.isBlank()) sb.append(textoMenu).append("\n\n");
                        for (int i = 0; i < opcoesInterp.size(); i++) {
                            sb.append(i + 1).append(". ").append(opcoesInterp.get(i)).append("\n");
                        }
                        sessao.setVariaveis(vars);
                        enviarTexto(instanceName, telefone, sb.toString().trim());
                        atualizarSessao(sessao, no.id(), true);
                        return;
                    }
                }

                case "entrada_dados" -> {
                    // campo no frontend: data.prompt, data.variavelSaida
                    String pergunta = interpolar(str(data, "prompt"), vars);
                    if (sessao.isAguardandoInput()) {
                        // Salva o input na variável configurada
                        String varSaida = str(data, "variavelSaida");
                        if (varSaida != null && !varSaida.isBlank() && inputUsuario != null) {
                            vars.put(varSaida, inputUsuario.trim());
                            sessao.setVariaveis(vars);
                        }
                        sessao.setAguardandoInput(false);
                    } else {
                        if (pergunta != null && !pergunta.isBlank()) {
                            sessao.setVariaveis(vars);
                            enviarTexto(instanceName, telefone, pergunta);
                        }
                        atualizarSessao(sessao, no.id(), true);
                        return;
                    }
                }

                case "receber_anexo" -> {
                    // campo no frontend: data.mensagem, data.variavelSaida
                    String instrucao = interpolar(str(data, "mensagem"), vars);
                    if (sessao.isAguardandoInput()) {
                        // Salva referência ao anexo (URL ou texto) na variável
                        String varSaida = str(data, "variavelSaida");
                        if (varSaida != null && !varSaida.isBlank() && inputUsuario != null) {
                            vars.put(varSaida, inputUsuario.trim());
                            sessao.setVariaveis(vars);
                        }
                        sessao.setAguardandoInput(false);
                    } else {
                        if (instrucao != null && !instrucao.isBlank()) {
                            sessao.setVariaveis(vars);
                            enviarTexto(instanceName, telefone, instrucao);
                        }
                        atualizarSessao(sessao, no.id(), true);
                        return;
                    }
                }

                case "condicao" -> {
                    // campos no frontend: data.variavel, data.operador (==, !=, contains, startsWith, isEmpty)
                    String nomeVariavel = str(data, "variavel");
                    String operador     = str(data, "operador");
                    String valor        = interpolar(str(data, "valor"), vars);
                    // Sujeito: valor da variável salva, ou o inputUsuario como fallback
                    String sujeito = (nomeVariavel != null && !nomeVariavel.isBlank())
                            ? vars.getOrDefault(nomeVariavel, "")
                            : (inputUsuario != null ? inputUsuario.trim() : "");
                    boolean resultado = avaliarCondicao(sujeito, operador, valor);
                    proximoHandle = resultado ? "true" : "false";
                }

                case "ir_para" -> {
                    // campo no frontend: data.destino (nodeId alvo)
                    String noAlvoId = str(data, "destino");
                    if (noAlvoId != null) {
                        FluxoNo noAlvo = encontrarNo(fluxo, noAlvoId);
                        if (noAlvo != null) {
                            no = noAlvo;
                            continue;
                        }
                    }
                }

                case "ir_para_fluxo" -> {
                    encerrarSessao(sessao);
                    return;
                }

                case "smart_delay" -> {
                    // campos no frontend: data.duracao (number), data.unidadeDelay ("segundos"|"minutos"|"horas")
                    Object duracaoObj = data.get("duracao");
                    String unidade    = str(data, "unidadeDelay");
                    if (duracaoObj instanceof Number n) {
                        long ms = n.longValue() * switch (unidade != null ? unidade : "segundos") {
                            case "minutos" -> 60_000L;
                            case "horas"   -> 3_600_000L;
                            default        -> 1_000L;   // segundos
                        };
                        long limite2 = Math.min(ms, 30_000L); // máx 30s por segurança
                        if (limite2 > 0) {
                            try { Thread.sleep(limite2); } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                }

                case "definir_valor" -> {
                    // campos no frontend: data.nomeVariavel, data.valorVariavel
                    String nomeVar = str(data, "nomeVariavel");
                    String valVar  = interpolar(str(data, "valorVariavel"), vars);
                    if (nomeVar != null && !nomeVar.isBlank()) {
                        vars.put(nomeVar, valVar != null ? valVar : "");
                        sessao.setVariaveis(vars);
                    }
                }

                case "script", "rpa", "nota" -> {
                    // Nós sem ação de envio — apenas passagem
                }

                case "requisicao_url" -> {
                    log.debug("[MOTOR] nó requisicao_url ainda não implementado, avançando");
                }

                case "integracoes" -> {
                    log.debug("[MOTOR] nó integracoes ainda não implementado, avançando");
                }

                case "ancora" -> {
                    // Âncora é referência visual, avança normalmente
                }

                case "fim" -> {
                    encerrarSessao(sessao);
                    return;
                }

                default -> log.warn("[MOTOR] Tipo de nó desconhecido: {}", tipo);
            }

            // Avança para próximo nó pelo handle correto
            no = proximoNo(fluxo, no.id(), proximoHandle);
            proximoHandle = null;
        }

        // Saiu do loop sem encontrar FIM — salva o nó atual
        if (no != null) {
            atualizarSessao(sessao, no.id(), false);
        } else {
            encerrarSessao(sessao);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers de navegação
    // ─────────────────────────────────────────────────────────────────────────

    /** Retorna o próximo nó seguindo a aresta com o handle especificado (ou qualquer handle se null). */
    private FluxoNo proximoNo(FluxoData fluxo, String noAtualId, String sourceHandle) {
        if (fluxo.edges() == null) return null;
        for (FluxoAresta aresta : fluxo.edges()) {
            if (!noAtualId.equals(aresta.source())) continue;
            if (sourceHandle != null && !sourceHandle.equals(aresta.sourceHandle())) continue;
            return encontrarNo(fluxo, aresta.target());
        }
        // Se nenhuma aresta com o handle especificado, tenta qualquer saída
        if (sourceHandle != null) {
            for (FluxoAresta aresta : fluxo.edges()) {
                if (noAtualId.equals(aresta.source())) {
                    return encontrarNo(fluxo, aresta.target());
                }
            }
        }
        return null;
    }

    private FluxoNo encontrarNo(FluxoData fluxo, String id) {
        if (fluxo.nodes() == null || id == null) return null;
        return fluxo.nodes().stream().filter(n -> id.equals(n.id())).findFirst().orElse(null);
    }

    private FluxoNo encontrarNoPorTipo(FluxoData fluxo, String tipo) {
        if (fluxo.nodes() == null) return null;
        return fluxo.nodes().stream().filter(n -> tipo.equals(n.type())).findFirst().orElse(null);
    }

    /** Mapeia a resposta do usuário para o sourceHandle da opção do menu ("op-0", "op-1"...). */
    private String resolverOpcaoMenu(List<String> opcoes, String input) {
        // Por número (1, 2, 3...)
        try {
            int idx = Integer.parseInt(input.trim()) - 1;
            if (idx >= 0 && idx < opcoes.size()) return "op-" + idx;
        } catch (NumberFormatException ignored) {}

        // Por texto (match parcial case-insensitive)
        for (int i = 0; i < opcoes.size(); i++) {
            if (opcoes.get(i).toLowerCase().contains(input.toLowerCase())) return "op-" + i;
        }
        return null; // fallback → primeira aresta disponível
    }

    /** Avalia condição usando os operadores do frontend: ==, !=, contains, startsWith, isEmpty, isNotEmpty */
    private boolean avaliarCondicao(String input, String operador, String valor) {
        String sujeito = input != null ? input.trim() : "";
        if (operador == null) return false;
        return switch (operador) {
            case "=="          -> sujeito.equalsIgnoreCase(valor);
            case "!="          -> !sujeito.equalsIgnoreCase(valor);
            case "contains"    -> valor != null && sujeito.toLowerCase().contains(valor.toLowerCase());
            case "startsWith"  -> valor != null && sujeito.toLowerCase().startsWith(valor.toLowerCase());
            case "isEmpty"     -> sujeito.isEmpty();
            case "isNotEmpty"  -> !sujeito.isEmpty();
            // legados
            case "igual"       -> sujeito.equalsIgnoreCase(valor);
            case "contem"      -> valor != null && sujeito.toLowerCase().contains(valor.toLowerCase());
            case "comeca_com"  -> valor != null && sujeito.toLowerCase().startsWith(valor.toLowerCase());
            case "vazio"       -> sujeito.isEmpty();
            case "nao_vazio"   -> !sujeito.isEmpty();
            default            -> false;
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers de sessão
    // ─────────────────────────────────────────────────────────────────────────

    private ChatbotSessao criarSessao(String instanceName, UUID empresaId,
                                       String telefone, UUID fluxoId, String noId) {
        ChatbotSessao s = new ChatbotSessao();
        s.setInstanceName(instanceName);
        s.setEmpresaId(empresaId);
        s.setTelefone(telefone);
        s.setFluxoId(fluxoId);
        s.setNoAtualId(noId);
        s.setAguardandoInput(false);
        return sessaoRepository.save(s);
    }

    private void atualizarSessao(ChatbotSessao sessao, String noId, boolean aguardando) {
        sessao.setNoAtualId(noId);
        sessao.setAguardandoInput(aguardando);
        sessaoRepository.save(sessao);
    }

    private void encerrarSessao(ChatbotSessao sessao) {
        sessaoRepository.delete(sessao);
        // Chatbot finalizado → EM_ESPERA (expira em 24 h pelo scheduler)
        conversaRepository.findByEmpresaIdAndTelefoneAndStatusIn(
                sessao.getEmpresaId(), sessao.getTelefone(),
                List.of(StatusConversa.EM_CHATBOT))
                .ifPresent(c -> {
                    c.setStatus(StatusConversa.EM_ESPERA);
                    conversaRepository.save(c);
                });
        log.debug("[MOTOR] Sessão encerrada, conversa em EM_ESPERA: instance={} telefone={}",
                   sessao.getInstanceName(), sessao.getTelefone());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers de envio
    // ─────────────────────────────────────────────────────────────────────────

    private void enviarTexto(String instanceName, String telefone, String texto) {
        try {
            EnviarTextoRequest req = new EnviarTextoRequest();
            req.setNumber(telefone);
            req.setText(texto);
            if (texto != null && (texto.contains("http://") || texto.contains("https://"))) {
                req.setLinkPreview(true);
            }
            evolutionApiClient.enviarTexto(instanceName, req);
            // Registra mensagem enviada pelo bot na conversa
            registrarMensagemNaConversa(
                    sessaoRepository.findByInstanceNameAndTelefone(instanceName, telefone)
                            .map(ChatbotSessao::getEmpresaId).orElse(null),
                    telefone, texto, true);
        } catch (Exception e) {
            log.error("[MOTOR] Erro ao enviar texto para {}: {}", telefone, e.getMessage());
        }
    }

    /** Registra uma mensagem na conversa EM_CHATBOT ativa para o telefone. */
    private void registrarMensagemNaConversa(UUID empresaId, String telefone,
                                              String texto, boolean enviada) {
        if (empresaId == null || texto == null) return;
        conversaRepository.findByEmpresaIdAndTelefoneAndStatusIn(
                empresaId, telefone, List.of(StatusConversa.EM_CHATBOT))
                .ifPresent(conversa -> {
                    MensagemDaConversa msg = MensagemDaConversa.builder()
                            .conversa(conversa)
                            .texto(texto)
                            .recebidaEm(Instant.now())
                            .enviada(enviada)
                            .build();
                    mensagemDaConversaRepository.save(msg);
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Interpolação de variáveis  {{nome_variavel}}
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Substitui {@code {{chave}}} no template pelo valor correspondente em {@code vars}.
     * Retorna o próprio template se null ou sem variáveis.
     */
    private String interpolar(String template, Map<String, String> vars) {
        if (template == null || vars == null || vars.isEmpty()) return template;
        String resultado = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            resultado = resultado.replace(
                    "{{" + entry.getKey() + "}}",
                    entry.getValue() != null ? entry.getValue() : "");
        }
        return resultado;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers de extração de data
    // ─────────────────────────────────────────────────────────────────────────

    private String str(Map<String, Object> data, String key) {
        Object v = data.get(key);
        return v instanceof String s ? s : null;
    }

    private Double num(Map<String, Object> data, String key) {
        Object v = data.get(key);
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
