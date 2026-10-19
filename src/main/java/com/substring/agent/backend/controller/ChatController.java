package com.substring.agent.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.substring.agent.backend.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE, MediaType.ALL_VALUE})
    public ResponseEntity<String> chat(
            @RequestHeader(value = "Conversation-Id", required = false) String conversationId,
            @RequestBody String message) {
        String cleanMessage = extractMessage(message);
        if (cleanMessage.isBlank()) {
            return ResponseEntity.badRequest().body("Message cannot be empty.");
        }
        String chatId = conversationId == null || conversationId.isBlank()
                ? java.util.UUID.randomUUID().toString()
                : conversationId.trim();
        return ResponseEntity.ok(chatService.chat(cleanMessage, chatId));
    }

    private String extractMessage(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                JsonNode node = objectMapper.readTree(trimmed);
                if (node.hasNonNull("message")) {
                    return node.get("message").asText().trim();
                } else if (node.hasNonNull("query")) {
                    return node.get("query").asText().trim();
                } else if (node.hasNonNull("prompt")) {
                    return node.get("prompt").asText().trim();
                }
            } catch (Exception ignored) {
                // Fall back to trimmed string if not JSON
            }
        }
        return trimmed;
    }
}
