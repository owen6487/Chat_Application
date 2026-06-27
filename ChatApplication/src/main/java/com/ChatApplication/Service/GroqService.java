package com.ChatApplication.Service;

import com.ChatApplication.Model.ChatMessagesModel;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class GroqService {
    @Value("${groq.chat.apiurl}")
    private String groqApiUrl;

    @Value("${groq.chat.model}")
    private String groqModel;

    @Value("${groq.chat.apikey}")
    private String groqApiKey;

    @Value("${groq.chat.max-tokens}")
    private Integer maxTokens;

    private final RestClient restClient;

    public GroqService(RestClient restClient) {
        this.restClient = restClient;
    }

    private static final String SYSTEM_PROMPT = """
              You are a helpful assistant that helps students in Kenya get information about universities.
            
                    You ONLY answer questions related to:
                    - Kenyan universities and colleges (public and private)
                    - Admission requirements and processes
                    - Courses, degrees, and faculties offered
                    - Tuition fees and scholarships
                    - Campus life and locations
                    
                     FORMATTING RULES — follow these strictly:
                        -when listing items ,put each item on it own line starting with numbers or dashed or bulleted 
                        -Never return a wall of text for list-type answers
                        - Keep responses concise and scannable
                        - Use short paragraphs, not one long block
                     
                     CONTEXT:
                         - You have access to the full conversation history
                         - Use it to understand follow-up questions like "tell me more" or "what about fees?"
                         - Always answer relative to the most recently discussed university or topic
            
                         GRADE-BASED RECOMMENDATIONS:
                         - If a student shares their grades (KCSE results, cluster points, mean grade, etc.)\s
                           and asks for recommendations, suggest suitable courses AND universities in Kenya
                         - Base recommendations only on Kenyan university admission requirements
            
                      
                     If the user greets you, you must respond with exactly this message:
                     "Hello! How can I help you find information about specific Kenyan universities or colleges today?"
                    If a user asks ANYTHING outside these topics, you must respond with EXACTLY this message:
                    "I can only help with questions about universities in Kenya.\s
                    Please ask me about admissions, courses, campuses, or scholarships."
            
                    Do not apologize. Do not explain further. Do not answer the off-topic question  under any circumstances.
            """;

    private final ConcurrentHashMap<String , Bucket> rateLimiters = new ConcurrentHashMap<>();
    private Bucket getRateLimiterBucket(String userId) {
        return rateLimiters.computeIfAbsent(userId ,Id ->{
            Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
            return Bucket.builder().addLimit(limit).build();
        });
    }

    /**
     * Sends a message to Groq AI and returns the assistant's reply.
     *
     * @param userId      the ID of the user making the request (for rate limiting)
     * @param userMessage the new message typed by the user
     * @param chatHistory all previous messages in the conversation
     * @return the AI assistant's reply as a String
     * @throws RuntimeException if rate limit is exceeded or Groq API fails
     */
    public String getResponse(String userMessage, String userId,List<ChatMessagesModel> chatHistory) {
        Bucket bucket =getRateLimiterBucket(userId);
          if (!bucket.tryConsume(1)){
              throw new RuntimeException("too much request");
          }


       List<Map<String, String>> messages = new ArrayList<>();
       
       // Fix: Map.of throws NullPointerException if any value is null. Using HashMap is safer.
       Map<String, String> systemMsg = new java.util.HashMap<>();
       systemMsg.put("role", "system");
       systemMsg.put("content", SYSTEM_PROMPT);
       messages.add(systemMsg);
       
       for(ChatMessagesModel chatMessage : chatHistory) {
           Map<String, String> msgMap = new java.util.HashMap<>();
           msgMap.put("role", chatMessage.getRole() != null ? chatMessage.getRole() : "user");
           msgMap.put("content", chatMessage.getMessage() != null ? chatMessage.getMessage() : "");
           messages.add(msgMap);
       }
       // Fix: Removed duplicate appending of userMessage. The current new message is 
       // already saved in chatHistory and was added in the loop above!

      // Fix: Map.of throws NullPointerException if any value is null. Using HashMap is safer.
      Map<String, Object> requestBody = new java.util.HashMap<>();
      requestBody.put("model", groqModel);
      requestBody.put("max_tokens", maxTokens);
      requestBody.put("messages", messages);
      requestBody.put("temperature", 0.3);
      @SuppressWarnings("unchecked")
       Map<String, Object> response = restClient.post()
            .uri(groqApiUrl)
            .header("Authorization", "Bearer " + groqApiKey)
            .header("Content-Type", "application/json")
            .body(requestBody)
            .retrieve()
            .body(Map.class);
       if(response ==null)

       {
           throw new RuntimeException("Error getting response from Groq API");
       }
       return extractTextFromResponse(response);
    }

    private String extractTextFromResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null) {
                    // Fix: message.get("content") can be null and throw NPE on .toString()
                    Object content = message.get("content");
                    return content != null ? content.toString() : "";
                }
            }
            throw new RuntimeException("Error getting response from Groq API");

        } catch (Exception e) {
            throw new RuntimeException("Error parsing response from Groq API", e);
        }
    }
}


