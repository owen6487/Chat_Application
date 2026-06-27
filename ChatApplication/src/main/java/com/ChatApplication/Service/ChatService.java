package com.ChatApplication.Service;

import com.ChatApplication.Model.ChatMessagesModel;
import com.ChatApplication.Repository.ChatRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {
    private final ChatRepository chatRepository;
    public ChatService(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }



    public ChatMessagesModel receivedMessage(ChatMessagesModel receivedMessage) {
       boolean existsByUserId = receivedMessage.getUserId() != null;
       boolean existsBySessionId = receivedMessage.getSessionId() != null;
       boolean existsByChatId = receivedMessage.getChatId() != null;
       // Fix: We must check that the actual message content is not null, otherwise Map.of will throw NPE
       boolean existsByMessage = receivedMessage.getMessage() != null && !receivedMessage.getMessage().trim().isEmpty();

       if(!existsByUserId || !existsBySessionId || !existsByChatId || !existsByMessage){
           throw new IllegalArgumentException("UserId, SessionId, ChatId, and Message must not be null or empty");
       }
       receivedMessage.setRole("user");

       chatRepository.save(receivedMessage);
       return receivedMessage;

    }

    public void saveAiResponse(String sessionId, String userId, String chatId, String aiReply) {
        ChatMessagesModel aiMessage = new ChatMessagesModel();
        aiMessage.setUserId(userId);
        aiMessage.setChatId(chatId);
        aiMessage.setMessage(aiReply);
        aiMessage.setSessionId(sessionId);
        aiMessage.setRole("assistant");

        chatRepository.save(aiMessage);
    }

    public List<ChatMessagesModel> getChatHistory(String chatId) {
        boolean chatExists = chatRepository.existsByChatId(chatId);
        if(!chatExists){
           return new ArrayList<>();
        }
        return chatRepository.findByChatId(chatId);
    }
}
