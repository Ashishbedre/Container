package containers.example.containers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class Helper {
    public String createAuthHeader() {
        try {
            Map<String, String> authConfig = new HashMap<>();
            authConfig.put("username", "ashishbedre");
            authConfig.put("password", "123456789");

            ObjectMapper objectMapper = new ObjectMapper();
            String authJson = objectMapper.writeValueAsString(authConfig);
            return Base64.getEncoder().encodeToString(authJson.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create auth header", e);
        }
    }
}
