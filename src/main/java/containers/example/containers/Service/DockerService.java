package containers.example.containers.Service;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import reactor.netty.http.client.HttpClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class DockerService {

    private final WebClient webClient;

    @Value("${cert.path}")
    private Resource certPath; // Path to client-cert.pem

    @Value("${key.path}")
    private Resource keyPath; // Path to client-key.pem

    @Autowired
    public DockerService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }
    public Mono<String> getDockerInfo() {
        // Load certificates from resources folder
        Resource certResource = new ClassPathResource("client-cert.pem");
        Resource keyResource = new ClassPathResource("client-key.pem");
        Resource caCertResource = new ClassPathResource("ca.pem");

        File certFile;
        File keyFile;
        File caCertFile;
        try {
            certFile = certResource.getFile();
            keyFile = keyResource.getFile();
            caCertFile = caCertResource.getFile();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load SSL certificate files from resources", e);
        }

        // Create an SSL context with custom key and trust managers
        SslContextBuilder sslContextBuilder = SslContextBuilder.forClient()
                .keyManager(certFile, keyFile)
                .trustManager(caCertFile);

        HttpClient httpClient = HttpClient.create()
                .secure(sslSpec -> sslSpec.sslContext(sslContextBuilder));

        WebClient webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        // Perform the GET request with the Host header including port
        return webClient.get()
                .uri("https://172.16.0.3:2376/containers/json")
                .header("Host", "172.16.0.3:2376")  // Include port in the Host header
                .retrieve()
                .bodyToMono(String.class);
    }


}


