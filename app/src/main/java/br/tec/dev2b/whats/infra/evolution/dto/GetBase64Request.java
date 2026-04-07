package br.tec.dev2b.whats.infra.evolution.dto;

import lombok.Data;

/**
 * Request body para POST /chat/getBase64FromMediaMessage/{instance}.
 *
 * <pre>
 * {
 *   "message": { "key": { "id": "<messageId>" } },
 *   "convertToMp4": false
 * }
 * </pre>
 */
@Data
public class GetBase64Request {

    private MessagePayload message;
    private boolean convertToMp4 = false;

    public static GetBase64Request of(String messageId) {
        GetBase64Request req = new GetBase64Request();
        MessagePayload mp  = new MessagePayload();
        KeyPayload     key = new KeyPayload();
        key.setId(messageId);
        mp.setKey(key);
        req.setMessage(mp);
        return req;
    }

    @Data
    public static class MessagePayload {
        private KeyPayload key;
    }

    @Data
    public static class KeyPayload {
        private String id;
    }
}
