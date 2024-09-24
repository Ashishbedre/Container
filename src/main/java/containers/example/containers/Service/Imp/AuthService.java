//package containers.example.containers.Service.Imp;
//
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Service
//public class AuthService {
//
//
//    private String authToken;  // Store the token here after login
//
//
//    // Login to Docker Hub and store the token
//    public String login(String username, String password) {
//        // Docker Hub login request body
//        Map<String, String> loginRequest = new HashMap<>();
//        loginRequest.put("username", username);
//        loginRequest.put("password", password);
//        WebClient webClient = WebClient.create();
//        // Make the login request
//        String token = webClient.post()
//                .uri("https://hub.docker.com/v2/users/login")
//                .bodyValue(loginRequest)
//                .retrieve()
//                .bodyToMono(String.class)
//                .block();  // Using block for simplicity, can use Mono for async
//
//        if (token != null) {
//            this.authToken = token;  // Store the token
//        }
//        return token;
//    }
//
//    // Get the stored token
//    public String getAuthToken() {
//        return authToken;
//    }
//
//    // Clear the token (e.g., for logout)
//    public void clearAuthToken() {
//        this.authToken = null;
//    }
//}
//
