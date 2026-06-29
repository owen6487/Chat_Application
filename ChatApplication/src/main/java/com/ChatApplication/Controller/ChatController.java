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

    private ChatService chatService;
    private GroqService groqService;
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
        info.put("MONGO_URI_present", System.getenv("MONGO_URI") != null ? "YES" : "NO");
        info.put("MONGO_URI_preview",
                System.getenv("MONGO_URI") != null
                        ? System.getenv("MONGO_URI").substring(0, Math.min(30, System.getenv("MONGO_URI").length()))
                        : "NULL");
        return ResponseEntity.ok(info);
    }
}
