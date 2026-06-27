package com.ChatApplication.Repository;

import com.ChatApplication.Model.ChatMessagesModel;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatRepository extends MongoRepository<ChatMessagesModel, String> {
     boolean existsByChatId(String chatId);
     boolean existsByUserId(String userId);
     boolean existsBySessionId(String sessionId);
     java.util.List<ChatMessagesModel> findByChatId(String chatId);
}
