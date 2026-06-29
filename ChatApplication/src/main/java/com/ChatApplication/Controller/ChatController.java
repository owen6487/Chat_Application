package com.ChatApplication.Controller;

import com.ChatApplication.Model.ChatMessagesModel;
import com.ChatApplication.Service.ChatService;
import com.ChatApplication.Service.GroqService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final GroqService groqService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public ChatController(SimpMessagingTemplate messagingTemplate, ChatService chatService, GroqService groqService) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
        this.groqService = groqService;
    }

    @MessageMapping("/send")
    public void sendMessage(ChatMessagesModel incomingMessage) {
        String destination = "/topic/chat/" + incomingMessage.getChatId();
        try {
            chatService.receivedMessage(incomingMessage);

            List<ChatMessagesModel> chaHistory = chatService.getChatHistory(incomingMessage.getChatId());

            String aiReply = groqService.getResponse(
                    incomingMessage.getMessage(),
                    incomingMessage.getUserId(),
                    chaHistory);
            // save the ai response (use the ChatService method that persists assistant
            // replies)
            chatService.saveAiResponse(
                    incomingMessage.getSessionId(),
                    incomingMessage.getUserId(),
                    incomingMessage.getChatId(),
                    aiReply);
            ChatMessagesModel assistantMsg = new ChatMessagesModel();
            assistantMsg.setChatId(incomingMessage.getChatId());
            assistantMsg.setUserId(incomingMessage.getUserId()); // or a special system id
            assistantMsg.setMessage(aiReply);
            assistantMsg.setSessionId(incomingMessage.getSessionId());
            assistantMsg.setRole("assistant");
            messagingTemplate.convertAndSend(destination, assistantMsg);
        } catch (IllegalArgumentException e) {
            messagingTemplate.convertAndSend(
                    destination + ".error",
                    "Bad Request" + e.getMessage());
        } catch (RuntimeException e) {
            // Fix: NPEs have a null message, so e.getMessage() is null. We must handle this
            // so the client doesn't see "Server error: null"
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();

            // 429 = Too Many Requests (standard HTTP code for rate limiting)
            messagingTemplate.convertAndSend(
                    destination + ".error",
                    message.contains("Rate limit")
                            ? "Too many requests: " + message

                            : "Server error: " + message);

        }

    }

    @GetMapping("/history")
    public List<ChatMessagesModel> getChatHistory(@RequestParam String chatId) {
        return chatService.getChatHistory(chatId);
    }

    @GetMapping("/debug/env")
    public ResponseEntity<Map<String, String>> debugEnv() {
        Map<String, String> info = new HashMap<>();

        // Check system environment variable
        String mongoUriEnv = System.getenv("MONGO_URI");
        info.put("MONGO_URI_env_present", mongoUriEnv != null ? "YES" : "NO");
        if (mongoUriEnv != null) {
            info.put("MONGO_URI_env_preview", mongoUriEnv.substring(0, Math.min(50, mongoUriEnv.length())) + "...");
        }

        // Check what Spring resolved (from any source: env, application.yaml, etc.)
        String mongoUriSpring = System.getProperty("spring.data.mongodb.uri");
        if (mongoUriSpring == null) {
            // Try to get from environment
            mongoUriSpring = mongoUriEnv;
        }
        info.put("MongoDB_URI_resolved", mongoUriSpring != null
                ? mongoUriSpring.substring(0, Math.min(50, mongoUriSpring.length())) + "..."
                : "NOT RESOLVED - CHECK ENV VARS");

        // Check Groq config
        info.put("GROQ_API_KEY_present", System.getenv("GROQ_API_KEY") != null ? "YES" : "NO");
        info.put("GROQ_API_URL_present", System.getenv("GROQ_API_URL") != null ? "YES" : "NO");

        info.put("Java_version", System.getProperty("java.version"));
        info.put("Active_profiles", String.join(",", org.springframework.core.env.Environment.class.getSimpleName()));

        return ResponseEntity.ok(info);
    }
}
