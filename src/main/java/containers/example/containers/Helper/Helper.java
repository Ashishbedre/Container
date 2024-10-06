package containers.example.containers.Helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class Helper {
    public String createAuthHeader(String username,String password ,String email, String serverAddress) {
        try {
            Map<String, String> authConfig = new HashMap<>();
            authConfig.put("username", username);
            authConfig.put("password", password);
            authConfig.put("email", email);
            authConfig.put("serverAddress", serverAddress);

            ObjectMapper objectMapper = new ObjectMapper();
            String authJson = objectMapper.writeValueAsString(authConfig);
            return Base64.getEncoder().encodeToString(authJson.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create auth header", e);
        }
    }
}
