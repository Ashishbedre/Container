//package containers.example.containers.Controller;
//
//import containers.example.containers.Service.Imp.AuthService;
//import containers.example.containers.dto.LoginRequest;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@CrossOrigin("*")
//@RequestMapping("/docker")
//public class AuthController {
//
//    private final AuthService authService;
//
//    public AuthController(AuthService authService) {
//        this.authService = authService;
//    }
//
//    // Endpoint for logging in
//    @PostMapping("/login")
//    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest) {
//        String token = authService.login(loginRequest.getUsername(), loginRequest.getPassword());
//
//        if (token != null) {
//            return ResponseEntity.ok("Login successful. Token stored.");
//        } else {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login failed.");
//        }
//    }
//
//    // Endpoint to clear the token (optional, for logout)
//    @PostMapping("/logout")
//    public ResponseEntity<String> logout() {
//        authService.clearAuthToken();
//        return ResponseEntity.ok("Logged out.");
//    }
//}
//
