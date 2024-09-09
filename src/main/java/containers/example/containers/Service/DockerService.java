package containers.example.containers.Service;

import com.fasterxml.jackson.databind.JsonNode;
import containers.example.containers.Entity.ContainerConfig;
import containers.example.containers.Entity.Deployment;
import containers.example.containers.Repository.ContainerConfigRepository;
import containers.example.containers.Repository.DeploymentRepository;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    private ContainerConfigRepository containerConfigRepository;

    @Autowired
    private DeploymentRepository deploymentRepository;
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


    private static final String DOCKER_API_URL = "https://172.16.0.3:2376/v1.46";




    public Deployment createContainer(ContainerConfig config) {
        String containerId = createDockerContainer(config);
        startDockerContainer(containerId);

        Deployment deployment = new Deployment();
        deployment.setDeploymentName(config.getName() + "-deployment");
        deployment.setContainerConfig(config);

        containerConfigRepository.save(config);
        return deploymentRepository.save(deployment);
    }

    public void stopContainer(String containerName) {
        String containerId = findContainerIdByName(containerName);
        stopDockerContainer(containerId);
    }

    private String createDockerContainer(ContainerConfig config) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("Image", config.getImageName() + ":" + config.getImageTag());
        requestBody.put("Name", config.getName());
        requestBody.put("Env", config.getEnv());
        requestBody.put("Cmd", config.getCmd());
        requestBody.put("ExposedPorts", Map.of(config.getExposedPort() + "/tcp", new HashMap<>()));
        requestBody.put("HostConfig", Map.of("PortBindings", Map.of(config.getHostPort() + "/tcp", new Object[]{Map.of("HostPort", config.getHostPort())})));
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

        return webClient.post()
                .uri(DOCKER_API_URL + "/containers/create")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(node -> node.get("Id").asText())
                .block();
    }

    private void startDockerContainer(String containerId) {
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

        webClient.post()
                .uri(DOCKER_API_URL + "/containers/{containerId}/start", containerId)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    private void stopDockerContainer(String containerId) {
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

        webClient.post()
                .uri(DOCKER_API_URL + "/containers/{containerId}/stop", containerId)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    private String findContainerIdByName(String containerName) {
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

        Map<String, Object> containerList = webClient.get()
                .uri(DOCKER_API_URL + "/containers/json")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        for (Map<String, Object> container : (Iterable<Map<String, Object>>) containerList.get("Containers")) {
            if (container.get("Names").equals("/" + containerName)) {
                return (String) container.get("Id");
            }
        }

        throw new RuntimeException("Container not found: " + containerName);
    }
}


